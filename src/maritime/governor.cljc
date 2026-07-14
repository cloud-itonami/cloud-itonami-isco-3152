(ns maritime.governor
  "MaritimeDeckGovernor — the independent safety/traceability layer
  for ship deck operations. Gates voyage planning, position reporting,
  navigational hazard flagging, and port-arrival coordination.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. vessel-registered    — the vessel must be registered before any operation.
    2. no-navigation-command — proposals must NEVER contain actual course/heading
                              commands, collision-avoidance decisions, or command
                              authority actions. Only administrative coordination
                              (planning, reporting, hazard surfacing) are permitted.
    3. effect-is-propose    — :effect must be :propose only (the governor
                              never directly executes operations).

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. :flag-navigational-hazard — any hazard flagging always escalates
                              to human review, regardless of confidence.
    5. low confidence (< confidence-floor)."
  (:require [maritime.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:flag-navigational-hazard})

(defn- hard-violations [{:keys [request proposal]}]
  (let [{:keys [vessel-id op]} proposal
        vessel-record (:vessel-record request)]
    (cond-> []
      (nil? vessel-record)
      (conj {:rule :no-vessel
             :detail "未登録 vessel — 登録していない船舶での作業不可"})

      (and (some #{:course-command :heading-command :collision-avoidance :master-command :command-authority}
                 [op]))
      (conj {:rule :no-navigation-command
             :detail "実航行・衝突回避・司令権の行使は禁止（master・desk officer の人間権限のみ）"})

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
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
