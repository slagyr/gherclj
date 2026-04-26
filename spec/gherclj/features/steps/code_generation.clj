(ns gherclj.features.steps.code-generation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.generator :as gen]
            [gherclj.sample.app-steps]
            [clojure.string :as str]))

(helper! gherclj.features.steps.code-generation)

(def ^:private pipeline-base-dir
  (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test"))

(defn setup-feature! [name source]
  (g/assoc! :feature-ir {:feature name :source source :scenarios []}))

(defn add-scenario! [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/update-in! [:feature-ir :scenarios] conj
                  {:scenario title :steps steps})))

(defn add-background! [table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/assoc-in! [:feature-ir :background] {:steps steps})))

(defn add-wip-scenario! [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/update-in! [:feature-ir :scenarios] conj
                  {:scenario title :steps steps :tags ["wip"]})))

(defn- ensure-framework-loaded! [fw]
  (let [lang (namespace fw)
        nm   (name fw)
        fw-ns (if lang
                (symbol (str "gherclj.frameworks." lang "." nm))
                (symbol (str "gherclj.frameworks." nm)))]
    (require fw-ns)))

(defn use-step-namespace! [ns-name]
  (let [ns-sym (symbol (str/replace ns-name #"^\"|\"$" ""))]
    (require ns-sym)
    (g/assoc! :step-ns ns-sym)))

(defn generate-spec! [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))
        step-ns (or (g/get :step-ns) 'gherclj.sample.app-steps)]
    (ensure-framework-loaded! fw)
    (let [config {:step-namespaces [step-ns]
                  :extra-steps (g/get :steps)
                  :framework fw}]
      (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir))))))

(defn output-should-contain [expected]
  (let [expected (str/replace expected #"^\"|\"$" "")
         raw-output (or (g/get :cli-output) (g/get :generated-output) (g/get :pipeline-output) "")
        output (-> raw-output
                   (str/replace #"\u001b\[[0-9;]*m" "")
                   (str/replace (str pipeline-base-dir "/") ""))]
    (g/should (str/includes? output expected))))

(defn output-should-not-contain [text]
  (let [raw-output (or (g/get :cli-output) (g/get :generated-output) (g/get :pipeline-output) "")
        output (-> raw-output
                   (str/replace #"\u001b\[[0-9;]*m" "")
                   (str/replace (str pipeline-base-dir "/") ""))]
    (g/should-not (str/includes? output text))))

(defn generated-code-should-be [doc-string]
  (g/should= (str/trim doc-string) (str/trim (g/get :generated-output))))

(defgiven "a feature named {name:string} from source {source:string}" code-generation/setup-feature!
  "Initializes :feature-ir with an empty scenarios list. source is the .feature file path embedded in generated code.")

(defgiven "a scenario {title:string} with steps:" code-generation/add-scenario!)

(defgiven "a background with steps:" code-generation/add-background!)

(defgiven "a wip scenario {title:string} with steps:" code-generation/add-wip-scenario!
  "Adds scenario with [\"wip\"] tag hardcoded — simulates a scenario tagged @wip.")

(defgiven "step namespace {ns-name:string}" code-generation/use-step-namespace!
  "Loads ns-name and uses it as the step namespace for the next generation. Defaults to gherclj.sample.app-steps if not set.")

(defwhen "generating the spec with framework {framework}" code-generation/generate-spec!
  "Uses the step namespace set by 'Given step namespace ...' or gherclj.sample.app-steps by default. Merges with any :extra-steps in state.")

(defthen #"^the output should contain (?!lines:$)(.+)$" code-generation/output-should-contain)

(defthen "the output should not contain {text:string}" code-generation/output-should-not-contain
  "Checks :cli-output → :generated-output → :pipeline-output in fallback order. Strips pipeline temp dir prefix from paths.")

(defthen "the generated code should be:" code-generation/generated-code-should-be)
