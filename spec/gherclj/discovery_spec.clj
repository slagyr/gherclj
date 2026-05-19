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
               (discovery/resolve-step-namespaces [] [])))

    (it "** as suffix crosses segment boundaries"
      (should= '[isaac.server.server-steps
                 isaac.comm.acp.acp-steps
                 isaac.config.cli.config-steps
                 isaac.acp-steps]
               (discovery/resolve-step-namespaces
                 ["isaac.**-steps"]
                 '[isaac.server.server-steps
                   isaac.comm.acp.acp-steps
                   isaac.config.cli.config-steps
                   isaac.acp-steps
                   isaac.server.server-helpers])))

    (it ".**. in the middle matches zero or more inner segments"
      (should= '[isaac.acp-steps
                 isaac.comm.acp-steps
                 isaac.comm.acp.acp-steps]
               (discovery/resolve-step-namespaces
                 ["isaac.**.acp-steps"]
                 '[isaac.acp-steps
                   isaac.comm.acp-steps
                   isaac.comm.acp.acp-steps
                   isaac.acp-helpers])))

    (it "**. at the start matches zero or more leading segments"
      (should= '[acp-steps
                 isaac.acp-steps
                 isaac.comm.acp.acp-steps]
               (discovery/resolve-step-namespaces
                 ["**.acp-steps"]
                 '[acp-steps
                   isaac.acp-steps
                   isaac.comm.acp.acp-steps
                   isaac.acp-helpers])))

    (it ".** at the end matches zero or more trailing segments"
      (should= '[isaac
                 isaac.comm
                 isaac.comm.acp.acp-steps]
               (discovery/resolve-step-namespaces
                 ["isaac.**"]
                 '[isaac
                   isaac.comm
                   isaac.comm.acp.acp-steps
                   isaacx])))

    (it "** matches across many dots"
      (should= '[a.b.c.d.e.steps]
               (discovery/resolve-step-namespaces
                 ["a.**.steps"]
                 '[a.b.c.d.e.steps])))

    (it "** combined with single * in same pattern"
      (should= '[isaac.comm.acp.acp-steps
                 isaac.server.foo.acp-steps]
               (discovery/resolve-step-namespaces
                 ["isaac.**.*-steps"]
                 '[isaac.comm.acp.acp-steps
                   isaac.server.foo.acp-steps
                   isaac.acp-helpers])))

    (it "** does not match malformed double-dot namespaces"
      (should= []
               (discovery/resolve-step-namespaces
                 ["isaac.**.steps"]
                 '[isaac..steps])))))
