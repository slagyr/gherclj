(ns gherclj.discovery-spec
  (:require [speclj.core :refer :all]
            [gherclj.discovery :as discovery]))

(describe "Discovery"

  (context "resolve-step-namespaces"

    (it "passes through concrete symbols"
      (should= '[myapp.steps.auth]
               (discovery/resolve-step-namespaces
                 '[myapp.steps.auth] [])))

    (it "expands glob patterns against available namespaces"
      (should= '[myapp.features.steps.auth myapp.features.steps.cart]
               (discovery/resolve-step-namespaces
                 ["myapp.features.steps.*"]
                 '[myapp.features.steps.auth myapp.features.steps.cart myapp.features.harness])))

    (it "supports glob in the middle"
      (should= '[myapp.auth-steps myapp.cart-steps]
               (discovery/resolve-step-namespaces
                 ["myapp.*-steps"]
                 '[myapp.auth-steps myapp.cart-steps myapp.auth-helpers])))

    (it "mixes concrete symbols and patterns"
      (should= '[myapp.manual.steps myapp.features.steps.auth]
               (discovery/resolve-step-namespaces
                 '[myapp.manual.steps "myapp.features.steps.*"]
                 '[myapp.features.steps.auth])))

    (it "returns empty for no config"
      (should= []
               (discovery/resolve-step-namespaces [] [])))))
