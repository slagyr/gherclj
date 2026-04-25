(ns space-airlock.features.helpers.airlock
  "Helpers for the airlock feature tests. Each function maps to one step
   phrase and contains the actual test logic. Step defs in
   space-airlock.features.steps.airlock are pure routing entries that
   forward to these functions."
  (:require [gherclj.core :as g]
            [space-airlock.airlock :as app]))

(defn- current-airlock []
  (or (g/get :airlock) (app/new-airlock)))

(defn- update-airlock! [f & args]
  (g/assoc! :airlock (apply f (current-airlock) args)))

(defn- door-key [door]
  (keyword (str door "-door")))

;; --- Given helpers ---

(defn crew-member-inside! [name]
  (g/assoc! :airlock (app/set-occupant (current-airlock) name :crew)))

(defn visitor-inside! []
  (g/assoc! :airlock (app/set-occupant (current-airlock) "visitor" :visitor)))

(defn wearing-suit! [name]
  (g/assoc! :airlock (-> (current-airlock)
                         (app/set-occupant name :crew)
                         (app/set-suit true))))

(defn not-wearing-suit! [name]
  (g/assoc! :airlock (-> (current-airlock)
                         (app/set-occupant name :crew)
                         (app/set-suit false))))

(defn valid-badge! [name]
  (g/assoc! :airlock (-> (current-airlock)
                         (app/set-occupant name :crew)
                         (app/set-badge true))))

(defn visitor-invalid-badge! []
  (g/assoc! :airlock (-> (current-airlock)
                         (app/set-occupant "visitor" :visitor)
                         (app/set-badge false))))

(defn door-state! [door state]
  (let [door (door-key door)]
    (case state
      "open"     (update-airlock! app/set-door-position door :open)
      "closed"   (update-airlock! app/set-door-position door :closed)
      "locked"   (update-airlock! app/set-door-lock     door :locked)
      "unlocked" (update-airlock! app/set-door-lock     door :unlocked))))

(defn chamber-pressure! [pressure]
  (update-airlock! app/set-pressure (keyword pressure)))

(defn emergency-override-engaged! []
  (update-airlock! app/engage-override))

;; --- When helpers ---

(defn request-exit! [name]
  (update-airlock! app/request-exit name))

(defn open-door! [door]
  (case door
    "inner" (update-airlock! app/command-open-inner-door)
    "outer" (update-airlock! app/command-open-outer-door)))

(defn depressurization-commanded! []
  (update-airlock! app/depressurize))

(defn repressurization-commanded! []
  (update-airlock! app/repressurize))

;; --- Then helpers ---

(defn chamber-should-depressurize []
  (g/should= :depressurized (:pressure (current-airlock))))

(defn chamber-should-remain [pressure]
  (g/should= (keyword pressure) (:pressure (current-airlock))))

(defn door-should-unlock [door]
  (g/should= :unlocked (get-in (current-airlock) [(door-key door) :lock])))

(defn door-should-remain-locked [door]
  (g/should= :locked (get-in (current-airlock) [(door-key door) :lock])))

(defn request-should-be-denied []
  (g/should (app/denied? (current-airlock))))

(defn system-should-display [message]
  (g/should= message (:message (current-airlock))))

(defn airlock-status-should-be [status]
  (g/should= status (app/status (current-airlock))))
