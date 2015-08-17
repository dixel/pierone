(ns org.zalando.stups.pierone.pierone-test
  (:require [org.zalando.stups.pierone.test-data :as d]
            [org.zalando.stups.pierone.test-utils :as u]
            [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [com.stuartsierra.component :as component]))

(deftest pierone-test
  (let [system (u/setup d/all-tags
                        d/all-images)
        root (first d/images-hierarchy)
        alt (second d/images-hierarchy)]

    (u/push-images d/images-hierarchy)

    ; tag root image as regular tag
    (u/expect 200 (client/put (u/v1-url "/repositories/" (:team d/tag)
                                        "/" (:artifact d/tag)
                                        "/tags/" (:name d/tag))
                              (u/http-opts (u/wrap-quotes (:id root))
                                           :json)))
    ; tag alt image as snapshot tag
    (u/expect 200 (client/put (u/v1-url "/repositories/" (:team d/snapshot-tag)
                                        "/" (:artifact d/snapshot-tag)
                                        "/tags/" (:name d/snapshot-tag))
                              (u/http-opts (u/wrap-quotes (:id alt))
                                           :json)))

    ; reverse image search
    (is (= 200
           (:status (client/get (u/p1-url "/tags/" (:id root))))))

    (let [result (-> (client/get (u/p1-url "/tags/" (:id root)))
                     (:body)
                     (json/read-str :key-fn keyword)
                     (first))]
        (is (= (:artifact result)
               (:artifact d/tag)))
        (is (= (:team result)
               (:team d/tag)))
        (is (= (:name result)
               (:name d/tag))))

    (is (= 404 (:status (client/get (u/p1-url "/tags/asdfa")
                                    (u/http-opts)))))
    (is (= 412 (:status (client/get (u/p1-url "/tags/img")
                                    (u/http-opts)))))


    ; check tag list for not existing artifact -> not ok
    (is (= 404 (:status (client/get (u/p1-url "/teams/"
                                              (:team d/tag)
                                              "/artifacts/asdfasdf"
                                              "/tags")
                                    (u/http-opts)))))

    (is (= 200 (:status (client/get (u/p1-url "/teams/" (:team d/tag)
                                              "/artifacts/" (:artifact d/tag)
                                              "/tags")
                                    (u/http-opts)))))

    ; check stats endpoint
    (let [resp (client/get (u/p1-url "/stats/teams/" (:team d/tag))
                           (u/http-opts))
          stats (json/read-str (:body resp)
                               :key-fn keyword)]
      (is (= 200 (:status resp)))
      (println stats)
      ; 3 images with 8 byte ("imgXdata") content
      (is (= (->> d/images-hierarchy
                  (map :data)
                  (map count)
                  (apply +))
             (:storage stats)))
      (is (= (count d/images-hierarchy)
             (:images stats)))
      ; kio is the only artifact
      (is (= 1
             (:artifacts stats)))
      ; regular tag and snapshot tag
      (is (= 2
             (:tags stats))))

    (let [resp (client/get (u/p1-url "/stats/teams")
                           (u/http-opts))
          stats (json/read-str (:body resp)
                               :key-fn keyword)]
      (is (= 200 (:status resp)))
      (is (= 1 (count stats)))
      (println stats)
      (is (:team (first stats))
          (:team d/tag)))

    ; stop
    (component/stop system)))
