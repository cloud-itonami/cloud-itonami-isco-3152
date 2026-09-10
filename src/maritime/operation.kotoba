(ns maritime.operation
  "The catalogue of operations the MaritimeDeckActor is permitted to
  coordinate — the closed allowlist the README has always claimed.

  This namespace exists because the governor could not be deny-by-default
  without it. Before it, `maritime.governor` refused command authority by
  naming five ops (`:course-command`, `:heading-command`,
  `:collision-avoidance`, `:master-command`, `:command-authority`) and
  admitted everything else. Measured on the unchanged governor, all five of
  `:alter-course`, `:override-autopilot`, `:issue-helm-order`,
  `:engine-order` and `:anything-at-all` reached `:commit` and wrote a
  record — including ops that are unmistakably helm and engine orders. A
  deny-list is only ever as wide as the names someone thought to write down;
  at sea the operations that matter are the ones nobody wrote down.

  So permission is a membership question about `catalogue`, not a
  non-membership question about a list of refusals. An op this catalogue
  does not name is refused, and adding an operation is a deliberate edit
  here rather than a side effect of inventing a keyword somewhere else.

  `named-command-authority` survives only to give a better refusal: those
  ops are refused by the same closed allowlist as everything else, but the
  operator is told *why* they are outside it rather than merely that they
  are.")

(def catalogue
  "Permitted operations, keyed by `:op`.

    :required   — payload keys without which the record would be a
                  meaningless entry in the vessel's operational history.
    :escalates? — always route to human sign-off, whatever the advisor's
                  confidence. Domain judgement the actor may surface but
                  must never settle.

  Every entry is administrative coordination. None of them moves a ship."
  {:log-position-report
   {:title      "Routine position and status report"
    :required   #{:latitude :longitude}
    :escalates? false
    :note       "Recording where the vessel was. Not deciding where it goes."}

   :draft-voyage-plan
   {:title      "Voyage-plan draft for the officer's own review and filing"
    :required   #{:departure-port :destination-port}
    :escalates? false
    :note       "A draft the deck officer reviews, amends and owns. The
                 actor never files a plan the officer has not read."}

   :flag-navigational-hazard
   {:title      "Surface a reported navigational hazard for human review"
    :required   #{:hazard-description}
    :escalates? true
    :note       "Always escalates. Weighing a hazard is the bridge team's
                 judgement; the actor's whole job here is to make sure a
                 human sees it."}

   :coordinate-port-arrival
   {:title      "Port-arrival logistics coordination"
    :required   #{:port}
    :escalates? false
    :note       "Berth, ETA and cargo notes ashore. Nothing on the bridge."}})

(def named-command-authority
  "Operations refused with a specific explanation rather than the generic
  one. They are outside `catalogue` like anything else — this map only
  changes the wording of the refusal, never the outcome."
  {:course-command      "針路の指令は master と当直航海士の人間権限"
   :heading-command     "船首方位の指令は master と当直航海士の人間権限"
   :collision-avoidance "衝突回避の判断・実行は bridge team（master・航海士・見張り）の専権"
   :master-command      "船長命令の代行は不可"
   :command-authority   "指揮権の行使は不可"})

(defn permitted?
  "Is `op` in the closed allowlist? Unknown ops — including `nil` — are not."
  [op]
  (contains? catalogue op))

(defn escalates?
  "Does `op` always require human sign-off regardless of confidence?"
  [op]
  (boolean (get-in catalogue [op :escalates?])))

(defn missing-fields
  "Required payload keys of `op` that `proposal` does not carry a
  non-nil value for. Empty for ops outside the catalogue — their refusal
  is `permitted?`'s to give, and reporting absent fields for an operation
  that is not allowed at all would describe the wrong problem."
  [op proposal]
  (into (sorted-set)
        (remove #(some? (get proposal %)))
        (get-in catalogue [op :required] #{})))

(defn refusal-detail
  "Why `op` is outside the allowlist, in the most specific terms available."
  [op]
  (or (get named-command-authority op)
      (str "許可された運用ではない — maritime.operation/catalogue に無い op "
           (pr-str op)
           "。許可されているのは "
           (pr-str (vec (sort (keys catalogue)))))))

(defn permitted-ops
  "The allowlist itself, for operators and for documentation that would
  otherwise drift away from the code."
  []
  (vec (sort (keys catalogue))))
