(ns gherclj.frameworks.speclj-spec
  (:require [gherclj.frameworks.speclj :as speclj-fw]
            [gherclj.generator :as gen]
            [speclj.core :refer :all]))

(describe "Speclj framework"

  (context "run-args"

    (it "appends framework options to the default generated spec args"
      (should= ["-c" "tmp/generated" "-s" "src" "-f" "documentation" "-c" "-P"]
               (speclj-fw/run-args {:output-dir "tmp/generated"
                                    :framework-opts ["-f" "documentation" "-c" "-P"]}))))

  (context "run-specs"

    (it "appends framework options to the default generated spec args"
      (let [captured (atom nil)]
        (with-redefs [speclj.cli/run (fn [& args]
                                       (reset! captured args)
                                       0)
                      gherclj.core/run-before-all-hooks! (fn [])
                      gherclj.core/run-after-all-hooks! (fn [])]
          (gen/run-specs {:test-framework :speclj
                          :output-dir "tmp/generated"
                          :framework-opts ["-f" "documentation" "-c" "-P"]}))

        (should= ["-c" "tmp/generated" "-s" "src" "-f" "documentation" "-c" "-P"]
                 @captured)))))
