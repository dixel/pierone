(ns org.zalando.stups.pierone.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.pierone.sql :as sql]
            [ring.util.response :as ring]
            [org.zalando.stups.friboo.ring :as fring]
            [org.zalando.stups.pierone.api-v2]
            [org.zalando.stups.pierone.api-v1]))

; define the API component and its dependencies
(def-http-component API "api/pierone-api.yaml" [db storage])

(def default-http-configuration
  {:http-port 8080})

(defn read-teams
  "Lists all teams that have artifacts."
  [_ _ db _]
  (let [result (map :team (sql/list-teams {} {:connection db}))]
    (-> (ring/response result)
        (fring/content-type-json))))

(defn read-artifacts
  "Lists all artifacts of a team."
  [parameters _ db _]
  (let [result (map :artifact (sql/list-artifacts parameters {:connection db}))]
    (-> (ring/response result)
        (fring/content-type-json))))

(defn read-tags
  "Lists all tags of an artifact."
  [parameters _ db _]
  (let [result (sql/list-tags parameters {:connection db})]
    (-> (ring/response result)
        (fring/content-type-json))))

(defn get-scm-source
  "Get SCM source information"
  [parameters _ db _]
  (let [result (first (sql/get-scm-source parameters {:connection db}))]
    (-> (ring/response result)
        (ring/status (if result 200 404))
        (fring/content-type-json))))