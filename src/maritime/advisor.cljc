(ns maritime.advisor
  "Maritime Deck Advisor: proposes voyage planning, position reporting,
  navigational hazard flagging, and port-arrival coordination based on vessel
  state and operational context. The advisor is subordinate to the governor;
  the governor gates every proposal.")

(defprotocol Advisor
  "Maritime deck advice layer."
  (-advise [this store request] "Advise on an operational request."))

(defn mock-advisor
  "Minimal advisor for testing: accepts any request and proposes it as-is."
  []
  (reify Advisor
    (-advise [_ _ request]
      (merge request {:effect :propose :confidence 0.8}))))

(defn basic-advisor
  "Maritime advisor with simple heuristics.
  Raises confidence for routine operations (position reporting, voyage planning).
  Lowers confidence for hazard flagging (requires domain knowledge)."
  []
  (reify Advisor
    (-advise [_ store request]
      (let [{:keys [op]} request
            conf (case op
                   :log-position-report 0.9
                   :draft-voyage-plan 0.75
                   :flag-navigational-hazard 0.4
                   :coordinate-port-arrival 0.8
                   0.5)]
        (assoc request :effect :propose :confidence conf)))))
