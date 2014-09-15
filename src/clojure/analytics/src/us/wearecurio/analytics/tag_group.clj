(ns us.wearecurio.analytics.tag-group
  (:require
    [us.wearecurio.analytics.database :as db]
    [clj-time.coerce :as c]
    [clj-time.core :as t]
    [korma.db :as kd]
    [korma.core :as kc]
    [environ.core :as e]))


;; Support for tag-groups.
;(defn get-id-for [model description]
;  (-> (kc/select* model)
;      (kc/fields :id)
;      (kc/where {:description description})
;      kc/select
;      last
;      :id))
;
;(defn get-tag-id-from-description [description]
;  (get-id-for db/tag description))
;
;(defn get-tag-group-id-from-description [description]
;  (get-id-for db/tag_group description))
;
;(defn immediate-children-tag-group-ids [parent-id]
;  (map :id (-> (kc/select* db/tag_group)
;               (kc/fields :id)
;               (kc/where {:parent_tag_group_id parent-id})
;               (kc/select))))
;
;(defn all-tag-group-ids []
;  (let [sql (-> (kc/select* db/tag_group)
;                (kc/fields :id)
;                (kc/order :id :DESC))]
;    (map :id (kc/select sql))))
;
;(defn get-tag-group-subtree-ids [parent-id]
;  (let [children (immediate-children-tag-group-ids parent-id)]
;    (if (nil? parent-id)
;        (flatten (map get-tag-group-subtree-ids children))
;        (cons parent-id (flatten (map get-tag-group-subtree-ids children))))))
;
;(defn tag-group-count []
;  (-> db/tag_group
;    (kc/fields ["count(*)" :count])
;    (kc/select)
;    first
;    :count))
;
;(defn tag-count-from-description [description]
;  (-> (kc/select* db/tag)
;    (kc/fields ["count(*)" :count])
;    (kc/where {:description description})
;    ;(kc/as-sql)))
;    (kc/select)
;    first
;    :count))
;
;(defn tag-group-create [description parent-id klass]
;  (kc/insert db/tag_group
;    (kc/values {:description description
;                :parent_tag_group_id parent-id
;                :class klass})))
;
;(defn tag-create [description]
;  (kc/insert db/tag
;    (kc/values {:description description})))
;
;(defn add-tag-to-tag-group [tag-name tag-group-name]
;  (let [tag-id       (get-tag-id-from-description tag-name)
;        tag-group-id (get-tag-group-id-from-description tag-group-name)]
;    (kc/insert db/tag_group_tag
;      (kc/values {:tag_group_tags_id tag-group-id
;                  :tag_id tag-id}))))
;
;(defn get-tag-ids-in-tag-group [tag-group-id]
;  (let [tag-group-ids (get-tag-group-subtree-ids tag-group-id)
;        result        (kc/select db/tag_group_tag
;                        (kc/where {:tag_group_tags_id [in tag-group-ids]}))]
;    (map :tag_id result)))
;
;(defn get-tag-ids-in-tag-group-from-description [tag-group-name]
;  (let [tag-group-id (get-tag-group-id-from-description tag-group-name)]
;    (get-tag-ids-in-tag-group tag-group-id)))
;
