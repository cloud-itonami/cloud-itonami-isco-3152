(ns maritime.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [maritime.actor :as actor]
            [maritime.store :as store]
            [maritime.advisor :as advisor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-vessel! st {:vessel-id "vessel-1" :name "MV Navigator"
                               :imo-number "9654321"})
    st))

(deftest commits-a-routine-position-report
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        request {:vessel-id "vessel-1"
                 :op :log-position-report
                 :latitude 35.6762
                 :longitude 139.6503}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "vessel-1"))))))

(deftest holds-operation-on-unregistered-vessel
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        request {:vessel-id "vessel-999"
                 :op :log-position-report
                 :latitude 0 :longitude 0}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "vessel-999")))))

(deftest interrupts-and-escalates-navigational-hazard-flagging
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/basic-advisor)})
        request {:vessel-id "vessel-1"
                 :op :flag-navigational-hazard
                 :hazard-description "Typhoon warning in area"
                 :severity :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "vessel-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "vessel-1")))))))

(deftest commits-voyage-plan-draft
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/basic-advisor)})
        request {:vessel-id "vessel-1"
                 :op :draft-voyage-plan
                 :departure-port "Tokyo"
                 :destination-port "Shanghai"
                 :estimated-duration-days 3}
        result (actor/run-request! graph request {} "thread-4")]
    (is (or (= :done (:status result))
            (= :interrupted (:status result))))
    (is (some? (get-in result [:state :proposal])))))

(deftest commits-port-arrival-coordination
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/basic-advisor)})
        request {:vessel-id "vessel-1"
                 :op :coordinate-port-arrival
                 :port "Singapore"
                 :eta-hours 24}
        result (actor/run-request! graph request {} "thread-5")]
    (is (or (= :done (:status result))
            (= :interrupted (:status result))))
    (is (some? (get-in result [:state :proposal])))))

(deftest rejects-course-command
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        request {:vessel-id "vessel-1"
                 :op :course-command
                 :heading 180}
        result (actor/run-request! graph request {} "thread-6")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "vessel-1")))))

(deftest rejects-collision-avoidance-command
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        request {:vessel-id "vessel-1"
                 :op :collision-avoidance
                 :action :alter-course}
        result (actor/run-request! graph request {} "thread-7")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "vessel-1")))))
