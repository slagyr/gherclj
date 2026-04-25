(ns space-airlock.airlock)

(defn new-airlock []
  {:occupant nil
   :pressure :pressurized
   :inner-door {:position :closed :lock :locked}
   :outer-door {:position :closed :lock :locked}
   :override? false
   :message nil
   :request-status nil
   :status nil})

(defn- ensure-occupant [state]
  (update state :occupant #(or % {:name nil :kind :crew :suit? false :badge-valid? false})))

(defn- with-occupant [state f & args]
  (apply update (ensure-occupant state) :occupant f args))

(defn set-occupant [state name kind]
  (let [current (:occupant state)]
    (assoc state :occupant {:name name
                           :kind kind
                           :suit? (boolean (:suit? current))
                           :badge-valid? (boolean (:badge-valid? current))})))

(defn set-suit [state suit?]
  (with-occupant state assoc :suit? suit?))

(defn set-badge [state badge-valid?]
  (with-occupant state assoc :badge-valid? badge-valid?))

(defn set-door-position [state door position]
  (assoc-in state [door :position] position))

(defn set-door-lock [state door lock]
  (assoc-in state [door :lock] lock))

(defn set-pressure [state pressure]
  (assoc state :pressure pressure))

(defn engage-override [state]
  (assoc state :override? true))

(defn- deny [state message]
  (-> state
      (assoc :request-status :denied)
      (assoc :message message)
      (assoc :status nil)))

(defn request-exit [state _name]
  (let [{:keys [suit? badge-valid?]} (:occupant state)]
    (cond
      (not badge-valid?) (deny state "Authorization required")
      (not suit?) (deny state "Suit required")
      :else (-> state
                (assoc :request-status :approved)
                (assoc :message nil)
                (assoc :pressure :depressurized)
                (assoc-in [:outer-door :lock] :unlocked)
                (assoc-in [:inner-door :lock] :locked)
                (assoc :status "cycle complete")))))

(defn command-open-inner-door [state]
  (cond
    (:override? state)
    (-> state
        (assoc-in [:inner-door :lock] :unlocked)
        (assoc :message "Emergency override active"))

    (= :depressurized (:pressure state))
    (-> state
        (assoc-in [:inner-door :lock] :locked)
        (assoc :message "Repressurize first"))

    :else
    (-> state
        (assoc-in [:inner-door :lock] :unlocked)
        (assoc :message nil))))

(defn command-open-outer-door [state]
  (cond
    (:override? state)
    (-> state
        (assoc-in [:outer-door :lock] :locked)
        (assoc :message "Outer door lock preserved"))

    (= :pressurized (:pressure state))
    (-> state
        (assoc-in [:outer-door :lock] :locked)
        (assoc :message "Depressurize first"))

    :else
    (-> state
        (assoc-in [:outer-door :lock] :unlocked)
        (assoc :message nil))))

(defn depressurize [state]
  (if (= :open (get-in state [:inner-door :position]))
    (-> state
        (assoc :pressure :pressurized)
        (assoc :message "Close inner door"))
    (-> state
        (assoc :pressure :depressurized)
        (assoc :message nil))))

(defn repressurize [state]
  (if (= :open (get-in state [:outer-door :position]))
    (-> state
        (assoc :pressure :depressurized)
        (assoc :message "Close outer door"))
    (-> state
        (assoc :pressure :pressurized)
        (assoc :message nil))))

(defn denied? [state]
  (= :denied (:request-status state)))

(defn status [state]
  (or (:status state)
      (cond
        (and (= :pressurized (:pressure state))
             (= :unlocked (get-in state [:inner-door :lock]))
             (= :locked (get-in state [:outer-door :lock])))
        "ready for boarding"

        (and (= :depressurized (:pressure state))
             (= :locked (get-in state [:inner-door :lock]))
             (= :unlocked (get-in state [:outer-door :lock])))
        "ready for exit"

        :else
        "idle")))
