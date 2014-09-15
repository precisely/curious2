(ns us.wearecurio.analytics.tag-group-test
  (:require [clojure.test :refer :all]
            [us.wearecurio.analytics.tag-group :as tg]
            [us.wearecurio.analytics.database :as db]
            [us.wearecurio.analytics.test-helpers :as th]
            [us.wearecurio.analytics.binify :as b]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [korma.db :as kd]
            [korma.core :as kc]))

(use-fixtures :once th/before-all)
(use-fixtures :each th/before-each)

;; Add support for iterating over tags in tag-groups.
;(deftest list_tag-group-ids
;  (testing "The table should be empty to start with."
;    (let [row {:class "us.wearecurio.model.TagGroup", :parent_tag_group_id nil, :description "almond group", :id 1}]
;      (is (= (tg/tag-group-count)
;             0))))
;
;  (testing "The count should be 1 if we inserted 1 tag_group."
;    (do (tg/tag-group-create "dairy" nil "us.wearecurio.model.TagGroup")
;        (is (= (tg/tag-group-count) 1)))))
;
;(deftest list_two_tag-group-ids
;  (testing "The count should be 2 if we insert 2 tag_groups."
;    (do
;        (tg/tag-group-create "food" nil "us.wearecurio.model.TagGroup")
;        (tg/tag-group-create "dairy" nil "us.wearecurio.model.TagGroup")
;
;        (is (= (tg/tag-group-count)
;               2))))
;
;  (testing "tag-group-ids should list the ids of the inserted tag groups."
;    (let [tag-group-ids (tg/all-tag-group-ids)]
;      (is (= (count tag-group-ids)
;             2))
;      (is (= (-> tag-group-ids first class)
;             Long))
;      (is (= (-> tag-group-ids last class)
;             Long)))))
;
;
;; Get all tags in a tag-group.
;(deftest list_tag-ids_in_tag-group
;  (testing "Assuming 2 tags in tag_group, tags-in-tag-group should return them."
;    (do
;      (tg/tag-group-create "food" nil "us.wearecurio.model.TagGroup")
;      (let [tag-id-pb    (:GENERATED_KEY (tg/tag-create "peanut butter"))
;            tag-id-jelly (:GENERATED_KEY (tg/tag-create "jelly"))]
;
;        ; There should only be 1 tag for "peanut butter" right now.
;        (is (= (tg/tag-count-from-description "peanut butter")
;               1))
;
;        (is (= (tg/get-tag-id-from-description "peanut butter")
;                tag-id-pb))
;
;        (is (= (tg/get-tag-id-from-description "jelly")
;               tag-id-jelly))
;
;        (tg/add-tag-to-tag-group "peanut butter" "food")
;        (tg/add-tag-to-tag-group "jelly" "food")
;
;        (is (= (set (tg/get-tag-ids-in-tag-group-from-description "food"))
;               (set (map :id (-> db/tag kc/select)))))))))
;
;
;;      TagGroups
;;
;;      Example TagGroup hierarchy
;;
;;                   nil
;;            ________|________
;;           /                 \
;;         Food              Exercise
;;      ____|____            ___|____
;;     /         \          /        \
;;   Meat        Dairy    Weights   Aerobic
;;
;; TagGroup --> Tags:
;;
;; Dairy                --> milk, cheese
;; Meat                 --> beef, pork
;; Food                 --> seaweed, peanut butter, jelly
;;
;; Exercise             --> gardening
;; Weights              --> rows, pushups
;; Aerobic              --> jogging, stairs
;;
;; Uncategorized tags   --> mood, san diego
;; Overlapping tag      --> chicken (Dairy & meat)
;;
;  (testing "If dairy is a subset of food, it should only return a subset of tag ids"
;    (do
;      ; Make dairy a child of food.
;      (let [tag-group-id-food      (:GENERATED_KEY (tg/tag-group-create "food" nil "us.wearecurio.model.TagGroup"))
;            tag-group-id-exercise  (:GENERATED_KEY (tg/tag-group-create "exercise" nil "us.wearecurio.model.TagGroup"))
;            tag-group-id-weights   (:GENERATED_KEY (tg/tag-group-create "weights" tag-group-id-exercise "us.wearecurio.model.TagGroup"))
;            tag-group-id-aerobic   (:GENERATED_KEY (tg/tag-group-create "aerobic" tag-group-id-exercise "us.wearecurio.model.TagGroup"))
;            tag-group-id-dairy     (:GENERATED_KEY (tg/tag-group-create "dairy" tag-group-id-food "us.wearecurio.model.TagGroup"))
;            tag-group-id-meat      (:GENERATED_KEY (tg/tag-group-create "meat" tag-group-id-food "us.wearecurio.model.TagGroup"))
;            tag-id-pb              (:GENERATED_KEY (tg/tag-create "peanut butter"))
;            tag-id-jelly           (:GENERATED_KEY (tg/tag-create "jelly"))
;            tag-id-seaweed         (:GENERATED_KEY (tg/tag-create "seaweed"))
;            tag-id-milk            (:GENERATED_KEY (tg/tag-create "milk"))
;            tag-id-cheese          (:GENERATED_KEY (tg/tag-create "cheese"))
;            tag-id-chicken         (:GENERATED_KEY (tg/tag-create "chicken"))
;            tag-id-beef            (:GENERATED_KEY (tg/tag-create "beef"))
;            tag-id-pork            (:GENERATED_KEY (tg/tag-create "pork"))
;            tag-id-mood            (:GENERATED_KEY (tg/tag-create "mood"))
;            tag-id-sd              (:GENERATED_KEY (tg/tag-create "san diego"))
;
;            tag-id-gardening       (:GENERATED_KEY (tg/tag-create "gardening"))
;            tag-id-rows            (:GENERATED_KEY (tg/tag-create "rows"))
;            tag-id-pushups         (:GENERATED_KEY (tg/tag-create "pushups"))
;            tag-id-jogging         (:GENERATED_KEY (tg/tag-create "jogging"))
;            tag-id-stairs          (:GENERATED_KEY (tg/tag-create "stairs"))
;            ]
;
;        ; Categorize food tags
;        (tg/add-tag-to-tag-group "seaweed"         "food")
;        (tg/add-tag-to-tag-group "peanut butter"   "food")
;        (tg/add-tag-to-tag-group "jelly"           "food")
;        (tg/add-tag-to-tag-group "milk"            "dairy")
;        (tg/add-tag-to-tag-group "cheese"          "dairy")
;        (tg/add-tag-to-tag-group "chicken"         "dairy")
;        (tg/add-tag-to-tag-group "chicken"         "meat")
;        (tg/add-tag-to-tag-group "beef"            "meat")
;        (tg/add-tag-to-tag-group "pork"            "meat")
;
;        ; Categorize exercise
;        (tg/add-tag-to-tag-group "gardening"       "exercise")
;        (tg/add-tag-to-tag-group "rows"            "weights")
;        (tg/add-tag-to-tag-group "pushups"         "weights")
;        (tg/add-tag-to-tag-group "jogging"         "aerobic")
;        (tg/add-tag-to-tag-group "stairs"          "aerobic")
;
;        ; Make sure tag-group-ids of the subtree are accessible by tag-group-id.
;        (is (= (set (tg/get-tag-group-subtree-ids nil))
;               (set (list tag-group-id-food tag-group-id-dairy tag-group-id-meat
;                        tag-group-id-exercise tag-group-id-weights tag-group-id-aerobic))))
;
;        (is (= (set (tg/get-tag-group-subtree-ids tag-group-id-food))
;               (set (list tag-group-id-food tag-group-id-dairy tag-group-id-meat))))
;
;        (is (= (set (tg/get-tag-group-subtree-ids tag-group-id-exercise))
;               (set (list tag-group-id-exercise tag-group-id-weights tag-group-id-aerobic))))
;
;
;        ; Get the tag-ids of the tag-group.
;        (is (= (set (tg/get-tag-ids-in-tag-group-from-description "dairy"))
;               (set (list tag-id-milk tag-id-cheese tag-id-chicken))))
;
;        ; Get all food tags.
;        (is (= (set (tg/get-tag-ids-in-tag-group-from-description "food"))
;               (set (list tag-id-pb tag-id-jelly tag-id-milk tag-id-cheese
;                          tag-id-chicken tag-id-beef tag-id-pork tag-id-seaweed))))
;
;        ; Get all aerobic exercise tags.
;        (is (= (set (tg/get-tag-ids-in-tag-group-from-description "aerobic"))
;               (set (list tag-id-jogging tag-id-stairs)))))))
;
