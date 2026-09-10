(ns maritime.governor
  "MaritimeDeckGovernor — the independent safety/traceability layer
  for ship deck operations. Gates voyage planning, position reporting,
  navigational hazard flagging, and port-arrival coordination.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. vessel-registered    — the vessel must be registered before any operation.
    2. operation-permitted  — the op must be in `maritime.operation/catalogue`,
                              the closed allowlist. Anything else is refused,
                              whether or not anyone anticipated it. Command
                              authority (course/heading orders, collision
                              avoidance, master's commands) is refused with a
                              specific explanation, but it is refused by the
                              same rule as an op nobody has ever seen.
    3. operation-complete   — the payload must carry the fields the catalogue
                              requires, so a committed record is a record of
                              something.
    4. effect-is-propose    — :effect must be :propose only (the governor
                              never directly executes operations).

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    5. ops the catalogue marks :escalates? true (`:flag-navigational-hazard`).
    6. low confidence (< confidence-floor).

  Invariant 2 was a deny-list until 2026-09-06. It named five forbidden ops
  and admitted every op it did not name, so `:alter-course`,
  `:override-autopilot`, `:issue-helm-order` and `:engine-order` all committed
  records on a registered vessel — measured, not hypothesised. The list of
  operations a ship's officer must never delegate is not a list anyone
  finishes writing, so the allowlist is the one that is enumerated."
  (:require [maritime.operation :as operation]
            [maritime.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]}]
  (let [{:keys [op]} proposal
        vessel-record (:vessel-record request)
        missing (operation/missing-fields op proposal)]
    (cond-> []
      (nil? vessel-record)
      (conj {:rule :no-vessel
             :detail "未登録 vessel — 登録していない船舶での作業不可"})

      (not (operation/permitted? op))
      (conj {:rule (if (contains? operation/named-command-authority op)
                     :no-navigation-command
                     :operation-not-permitted)
             :op op
             :detail (operation/refusal-detail op)})

      (seq missing)
      (conj {:rule :incomplete-operation
             :op op
             :missing (vec missing)
             :detail (str "必須項目が欠けている: " (pr-str (vec missing)))})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は直接実行しない）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `maritime.store/Store`. Pure — never mutates
  the store, never executes navigation commands."
  [request context proposal store]
  (let [vessel-id (:vessel-id proposal)
        vessel-record (store/vessel store vessel-id)
        request-with-records (merge request
                                    {:vessel-record vessel-record})
        hard (hard-violations {:request request-with-records :proposal proposal})
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (operation/escalates? (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
