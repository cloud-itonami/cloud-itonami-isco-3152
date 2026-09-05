(ns maritime.operation-test
  "Tests for the closed allowlist and the refusals it produces.

  Every negative test here asserts the *rule* the governor names, not
  merely that the proposal was held. A `:hold` on its own is not evidence
  that the allowlist did anything — an unregistered vessel, a missing
  field or a non-`:propose` effect all produce exactly the same `:hold`.
  So each test registers its vessel, supplies a complete payload, and then
  pins the literal rule keyword. If the allowlist stops being the reason,
  these go red even though the disposition is unchanged."
  (:require [clojure.test :refer [deftest is testing]]
            [maritime.actor :as actor]
            [maritime.advisor :as advisor]
            [maritime.governor :as governor]
            [maritime.operation :as operation]
            [maritime.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-vessel! st {:vessel-id "vessel-1" :name "MV Navigator"
                                :imo-number "9654321"})
    st))

(defn- run [st request thread-id]
  (actor/run-request! (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
                      request {} thread-id))

(defn- rules [result]
  (into #{} (map :rule) (get-in result [:state :verdict :violations])))

;; ---------------------------------------------------------------------------
;; The allowlist is closed.

(deftest refuses-command-authority-nobody-wrote-down
  (testing "ops that are plainly helm/engine orders but are named nowhere
            in the code. Before the catalogue existed every one of these
            reached :commit and wrote a record."
    (doseq [[op payload] {:alter-course       {:heading 270}
                          :override-autopilot {:mode :manual}
                          :issue-helm-order   {:order "hard to starboard"}
                          :engine-order       {:telegraph :full-astern}}]
      (testing (str op)
        (let [st (fresh-store)
              result (run st (merge {:vessel-id "vessel-1" :op op} payload)
                          (str "unnamed-" (name op)))]
          (is (= :hold (:disposition (:state result))))
          (is (contains? (rules result) :operation-not-permitted)
              "must be refused BY THE ALLOWLIST, not incidentally")
          (is (empty? (store/records-of st "vessel-1"))))))))

(deftest refuses-an-op-that-means-nothing
  (testing "a keyword with no meaning at all is still outside the allowlist
            — deny-by-default does not require the refusal to be anticipated"
    (let [st (fresh-store)
          result (run st {:vessel-id "vessel-1" :op :anything-at-all} "meaningless")]
      (is (= :hold (:disposition (:state result))))
      (is (contains? (rules result) :operation-not-permitted))
      (is (empty? (store/records-of st "vessel-1"))))))

(deftest refuses-a-missing-op
  (testing "no :op at all is not a permitted op"
    (let [st (fresh-store)
          result (run st {:vessel-id "vessel-1"} "no-op")]
      (is (= :hold (:disposition (:state result))))
      (is (contains? (rules result) :operation-not-permitted))
      (is (empty? (store/records-of st "vessel-1"))))))

(deftest named-command-authority-is-refused-with-its-own-reason
  (testing "the five named ops are refused by the same allowlist, but the
            operator is told which human authority they belong to"
    (doseq [op (keys operation/named-command-authority)]
      (testing (str op)
        (let [st (fresh-store)
              result (run st {:vessel-id "vessel-1" :op op} (str "named-" (name op)))
              violation (first (filter #(= :no-navigation-command (:rule %))
                                       (get-in result [:state :verdict :violations])))]
          (is (= :hold (:disposition (:state result))))
          (is (some? violation) "must carry the specific rule, not the generic one")
          (is (not (contains? (rules result) :operation-not-permitted))
              "one refusal, in the most specific terms available — not two")
          ;; Pin the literal text, not merely that some text is present.
          ;; The generic fallback is also a non-empty string, so asserting
          ;; `(seq (:detail violation))` here passed even with the specific
          ;; wording removed — measured 2026-09-06 against a build whose
          ;; `refusal-detail` never consulted `named-command-authority`.
          (is (= (get operation/named-command-authority op) (:detail violation))
              "the operator is told which human authority this belongs to")
          (is (not (re-find #"catalogue に無い op" (str (:detail violation))))
              "must not have fallen back to the generic refusal")
          (is (empty? (store/records-of st "vessel-1"))))))))

;; ---------------------------------------------------------------------------
;; A committed record is a record of something.

(deftest refuses-a-permitted-op-with-its-required-fields-missing
  (doseq [[op payload missing] [[:log-position-report {:latitude 35.6} [:longitude]]
                                [:draft-voyage-plan {:departure-port "Tokyo"} [:destination-port]]
                                [:coordinate-port-arrival {} [:port]]]]
    (testing (str op " missing " missing)
      (let [st (fresh-store)
            result (run st (merge {:vessel-id "vessel-1" :op op} payload)
                        (str "incomplete-" (name op)))
            violation (first (filter #(= :incomplete-operation (:rule %))
                                     (get-in result [:state :verdict :violations])))]
        (is (= :hold (:disposition (:state result))))
        (is (some? violation) "must be refused for incompleteness specifically")
        (is (= missing (:missing violation))
            "the refusal names which fields are absent")
        (is (empty? (store/records-of st "vessel-1")))))))

(deftest a-nil-required-field-counts-as-missing
  (testing "present-but-nil is not a value"
    (let [st (fresh-store)
          result (run st {:vessel-id "vessel-1" :op :log-position-report
                          :latitude 35.6 :longitude nil}
                      "nil-field")]
      (is (= :hold (:disposition (:state result))))
      (is (contains? (rules result) :incomplete-operation)))))

;; ---------------------------------------------------------------------------
;; The allowlist admits what it says it admits.

(deftest every-catalogued-op-is-admitted-when-complete
  (testing "the closed allowlist is not closed onto the empty set — each
            catalogued op reaches commit or human sign-off, never :hold"
    (doseq [[op payload] {:log-position-report      {:latitude 35.6762 :longitude 139.6503}
                          :draft-voyage-plan        {:departure-port "Tokyo"
                                                     :destination-port "Shanghai"}
                          :flag-navigational-hazard {:hazard-description "Typhoon warning"}
                          :coordinate-port-arrival  {:port "Singapore"}}]
      (testing (str op)
        (let [st (fresh-store)
              result (run st (merge {:vessel-id "vessel-1" :op op} payload)
                          (str "admit-" (name op)))]
          (is (not= :hold (:disposition (:state result)))
              (str op " is in the catalogue and must not be held"))
          (is (empty? (rules result))))))))

(deftest escalation-policy-comes-from-the-catalogue
  (testing "hazard flagging escalates because the catalogue says so, and the
            governor no longer keeps its own second copy of that fact"
    (is (true? (operation/escalates? :flag-navigational-hazard)))
    (is (false? (operation/escalates? :log-position-report)))
    (let [st (fresh-store)
          verdict (governor/check {} {}
                                  {:vessel-id "vessel-1"
                                   :op :flag-navigational-hazard
                                   :hazard-description "Typhoon warning"
                                   :effect :propose
                                   :confidence 0.99}
                                  st)]
      (is (false? (:ok? verdict)))
      (is (true? (:escalate? verdict)) "escalates even at 0.99 confidence")
      (is (false? (:hard? verdict))))))

;; ---------------------------------------------------------------------------
;; The catalogue as a value.

(deftest catalogue-is-well-formed
  (doseq [[op entry] operation/catalogue]
    (testing (str op)
      (is (keyword? op))
      (is (seq (:title entry)))
      (is (set? (:required entry)))
      (is (contains? entry :escalates?))
      (is (boolean? (:escalates? entry))))))

(deftest named-command-authority-is-disjoint-from-the-catalogue
  (testing "an op cannot be both permitted and refused by name"
    (is (empty? (filter operation/permitted?
                        (keys operation/named-command-authority))))))

(deftest missing-fields-is-silent-for-ops-outside-the-allowlist
  (testing "an op that is not allowed at all has no required fields to
            report — describing the wrong problem would bury the right one"
    (is (empty? (operation/missing-fields :alter-course {})))
    (is (empty? (operation/missing-fields nil {})))))

(deftest permitted-ops-matches-the-catalogue
  (is (= (vec (sort (keys operation/catalogue))) (operation/permitted-ops)))
  (is (every? operation/permitted? (operation/permitted-ops)))
  (is (false? (operation/permitted? :alter-course)))
  (is (false? (operation/permitted? nil))))
