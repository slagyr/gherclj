(ns gherclj.unused-spec
  (:require [gherclj.unused :as unused]
            [speclj.core :refer :all]))

(def sample-steps
  [{:type :given :template "a user {name:string}" :regex #"^a user .+$" :file "/tmp/app_steps.clj" :line 4}
   {:type :when :template "the user logs in" :regex #"^the user logs in$" :file "/tmp/app_steps.clj" :line 8}
   {:type :then :template "the response should be {status:int}" :regex #"^the response should be \d+$" :file "/tmp/app_steps.clj" :line 12}])

(describe "Unused"

  (context "analyze"

    (it "reports no unused steps when every step text is referenced"
      (let [irs [{:scenarios [{:scenario "Login"
                               :steps [{:type :given :text "a user \"alice\""}
                                       {:type :when :text "the user logs in"}
                                       {:type :then :text "the response should be 200"}]}]}]
            result (unused/analyze sample-steps irs {})]
        (should= 1 (:scanned-scenarios result))
        (should= 0 (:unscanned-scenarios result))
        (should= 3 (:used-step-count result))
        (should= 3 (:total-step-count result))
        (should= [] (:unused-steps result))))

    (it "treats steps in excluded scenarios as unused"
      (let [irs [{:scenarios [{:scenario "Login"
                               :steps [{:type :given :text "a user \"alice\""}
                                       {:type :when :text "the user logs in"}]}
                              {:scenario "Slow check"
                               :tags ["slow"]
                               :steps [{:type :then :text "the response should be 200"}]}]}]
            result (unused/analyze sample-steps irs {:exclude-tags ["slow"]})]
        (should= 1 (:scanned-scenarios result))
        (should= 1 (:unscanned-scenarios result))
        (should= 2 (:used-step-count result))
        (should= 3 (:total-step-count result))
        (should= ["the response should be {status:int}"] (mapv :template (:unused-steps result))))))

  (context "render"

    (it "renders a no-filter no-unused summary"
      (let [text (unused/render {:scanned-scenarios 1
                                 :unscanned-scenarios 0
                                 :used-step-count 3
                                 :total-step-count 3
                                 :unused-steps []
                                 :filters []})]
        (should (.contains text "Scanned 1 scenario. No tag filtering applied."))
        (should (.contains text "3 of 3 registered steps are in use."))
        (should (.contains text "No unused steps found."))))

    (it "renders filtered grouped unused output"
      (let [text (unused/render {:scanned-scenarios 1
                                 :unscanned-scenarios 1
                                 :used-step-count 1
                                 :total-step-count 3
                                 :unused-steps [(second sample-steps) (nth sample-steps 2)]
                                 :filters ["~slow"]})]
        (should (.contains text "Scanned 1 of 2 scenarios. 1 scenario filtered out by tags: ~slow."))
        (should (.contains text "1 of 3 registered steps are in use (2 unused)."))
        (should (.contains text "Unused steps:"))
        (should (.contains text "When:"))
        (should (.contains text "Then:"))
        (should (.contains text "the user logs in  (app_steps.clj:8)"))
        (should (.contains text "the response should be {status:int}  (app_steps.clj:12)"))))))
