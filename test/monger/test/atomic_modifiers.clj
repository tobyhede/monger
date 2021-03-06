(set! *warn-on-reflection* true)

(ns monger.test.atomic-modifiers
  (:import  [com.mongodb WriteResult WriteConcern DBCursor DBObject CommandResult$CommandFailure]
            [org.bson.types ObjectId]
            [java.util Date])
  (:require [monger core util]
            [monger.collection :as mgcol]
            [monger.result     :as mgres]
            [monger.test.helper :as helper])
  (:use [clojure.test]
        [monger.operators]
        [monger.test.fixtures]))

(use-fixtures :each purge-docs purge-things purge-scores)


;;
;; $inc
;;

(deftest increment-a-single-existing-field-using-$inc-modifier
  (let [coll "scores"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :username "l33r0y" :score 100 })
    (mgcol/update coll { :_id oid } { $inc { :score 20 } })
    (is (= 120 (:score (mgcol/find-map-by-id coll oid))))))

(deftest set-a-single-non-existing-field-using-$inc-modifier
  (let [coll "scores"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :username "l33r0y" })
    (mgcol/update coll { :_id oid } { $inc { :score 30 } })
    (is (= 30 (:score (mgcol/find-map-by-id coll oid))))))


(deftest increment-multiple-existing-fields-using-$inc-modifier
  (let [coll "scores"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :username "l33r0y" :score 100 :bonus 0 })
    (mgcol/update coll { :_id oid } {$inc { :score 20 :bonus 10 } })
    (is (= { :_id oid :score 120 :bonus 10 :username "l33r0y" } (mgcol/find-map-by-id coll oid)))))


(deftest increment-and-set-multiple-existing-fields-using-$inc-modifier
  (let [coll "scores"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :username "l33r0y" :score 100 })
    (mgcol/update coll { :_id oid } { $inc { :score 20 :bonus 10 } })
    (is (= { :_id oid :score 120 :bonus 10 :username "l33r0y" } (mgcol/find-map-by-id coll oid)))))



;;
;; $set
;;

(deftest update-a-single-existing-field-using-$set-modifier
  (let [coll "things"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :weight 10.0 })
    (mgcol/update coll { :_id oid } { $set { :weight 20.5 } })
    (is (= 20.5 (:weight (mgcol/find-map-by-id coll oid [:weight]))))))

(deftest set-a-single-non-existing-field-using-$set-modifier
  (let [coll "things"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :weight 10.0 })
    (mgcol/update coll { :_id oid } { $set { :height 17.2 } })
    (is (= 17.2 (:height (mgcol/find-map-by-id coll oid [:height]))))))

(deftest update-multiple-existing-fields-using-$set-modifier
  (let [coll "things"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :weight 10.0 :height 15.2 })
    (mgcol/update coll { :_id oid } { $set { :weight 20.5 :height 25.6 } })
    (is (= { :_id oid :weight 20.5 :height 25.6 } (mgcol/find-map-by-id coll oid [:weight :height])))))


(deftest update-and-set-multiple-fields-using-$set-modifier
  (let [coll "things"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :weight 10.0 })
    (mgcol/update coll { :_id oid } {$set { :weight 20.5 :height 25.6 } })
    (is (= { :_id oid :weight 20.5 :height 25.6 } (mgcol/find-map-by-id coll oid [:weight :height])))))


;;
;; $unset
;;

(deftest unset-a-single-existing-field-using-$unset-modifier
  (let [coll "docs"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :title "Document 1" :published true })
    (mgcol/update coll { :_id oid } { $unset { :published 1 } })
    (is (= { :_id oid :title "Document 1" } (mgcol/find-map-by-id coll oid)))))


(deftest unset-multiple-existing-fields-using-$unset-modifier
  (let [coll "docs"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :title "Document 1" :published true :featured true })
    (mgcol/update coll { :_id oid } { $unset { :published 1 :featured true } })
    (is (= { :_id oid :title "Document 1" } (mgcol/find-map-by-id coll oid)))))


(deftest unsetting-an-unexisting-field-using-$unset-modifier-is-not-considered-an-issue
  (let [coll "docs"
        oid  (ObjectId.)]
    (mgcol/insert coll { :_id oid :title "Document 1" :published true })
    (is (mgres/ok? (mgcol/update coll { :_id oid } { $unset { :published 1 :featured true } })))
    (is (= { :_id oid :title "Document 1" } (mgcol/find-map-by-id coll oid)))))


;;
;; $push
;;

(deftest initialize-an-array-using-$push-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$push modifier appends value to field"]
    (mgcol/insert coll { :_id oid :title title })
    (mgcol/update coll { :_id oid } { $push { :tags "modifiers" } })
    (is (= { :_id oid :title title :tags ["modifiers"] } (mgcol/find-map-by-id coll oid)))))

(deftest add-value-to-an-existing-array-using-$push-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$push modifier appends value to field"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb"] })
    (mgcol/update coll { :_id oid } { $push { :tags "modifiers" } })
    (is (= { :_id oid :title title :tags ["mongodb" "modifiers"] } (mgcol/find-map-by-id coll oid)))))


;; this is a common mistake, I leave it here to demonstrate it. You almost never
;; actually want to do this! What you really want is to use $pushAll instead of $push. MK.
(deftest add-array-value-to-an-existing-array-using-$push-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$push modifier appends value to field"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb"] })
    (mgcol/update coll { :_id oid } { $push { :tags ["modifiers" "operators"] } })
    (is (= { :_id oid :title title :tags ["mongodb" ["modifiers" "operators"]] } (mgcol/find-map-by-id coll oid)))))



(deftest double-add-value-to-an-existing-array-using-$push-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$push modifier appends value to field"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb"] })
    (mgcol/update coll { :_id oid } { $push { :tags "modifiers" } })
    (mgcol/update coll { :_id oid } { $push { :tags "modifiers" } })
    (is (= { :_id oid :title title :tags ["mongodb" "modifiers" "modifiers"] } (mgcol/find-map-by-id coll oid)))))

;;
;; $pushAll
;;

(deftest initialize-an-array-using-$pushAll-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$pushAll modifier appends multiple values to field"]
    (mgcol/insert coll { :_id oid :title title })
    (mgcol/update coll { :_id oid } { $pushAll { :tags ["mongodb" "docs"] } })
    (is (= { :_id oid :title title :tags ["mongodb" "docs"] } (mgcol/find-map-by-id coll oid)))))

(deftest add-value-to-an-existing-array-using-$pushAll-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$pushAll modifier appends multiple values to field"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb"] })
    (mgcol/update coll { :_id oid } { $pushAll { :tags ["modifiers" "docs"] } })
    (is (= { :_id oid :title title :tags ["mongodb" "modifiers" "docs"] } (mgcol/find-map-by-id coll oid)))))


(deftest double-add-value-to-an-existing-array-using-$pushAll-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$pushAll modifier appends multiple values to field"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb" "docs"] })
    (mgcol/update coll { :_id oid } { $pushAll { :tags ["modifiers" "docs"] } })
    (is (= { :_id oid :title title :tags ["mongodb" "docs" "modifiers" "docs"] } (mgcol/find-map-by-id coll oid)))))


;;
;; $addToSet
;;

(deftest initialize-an-array-using-$addToSet-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$addToSet modifier appends value to field unless it is already there"]
    (mgcol/insert coll { :_id oid :title title })
    (mgcol/update coll { :_id oid } { $addToSet { :tags "modifiers" } })
    (is (= { :_id oid :title title :tags ["modifiers"] } (mgcol/find-map-by-id coll oid)))))

(deftest add-value-to-an-existing-array-using-$addToSet-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$addToSet modifier appends value to field unless it is already there"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb"] })
    (mgcol/update coll { :_id oid } { $addToSet { :tags "modifiers" } })
    (is (= { :_id oid :title title :tags ["mongodb" "modifiers"] } (mgcol/find-map-by-id coll oid)))))


(deftest double-add-value-to-an-existing-array-using-$addToSet-modifier
  (let [coll  "docs"
        oid   (ObjectId.)
        title "$addToSet modifier appends value to field unless it is already there"]
    (mgcol/insert coll { :_id oid :title title :tags ["mongodb"] })
    (mgcol/update coll { :_id oid } { $addToSet { :tags "modifiers" } })
    (mgcol/update coll { :_id oid } { $addToSet { :tags "modifiers" } })
    (is (= { :_id oid :title title :tags ["mongodb" "modifiers"] } (mgcol/find-map-by-id coll oid)))))


;;
;; $pop
;;

(deftest pop-last-value-in-the-array-using-$pop-modifier
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$pop modifier removes last or first value in the array"]
    (mgcol/insert coll { :_id oid :title title :tags ["products" "apple" "reviews"] })
    (mgcol/update coll { :_id oid } { $pop { :tags 1 } })
    (is (= { :_id oid :title title :tags ["products" "apple"] } (mgcol/find-map-by-id coll oid)))))

(deftest unshift-first-value-in-the-array-using-$pop-modifier
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$pop modifier removes last or first value in the array"]
    (mgcol/insert coll { :_id oid :title title :tags ["products" "apple" "reviews"] })
    (mgcol/update coll { :_id oid } { $pop { :tags -1 } })
    (is (= { :_id oid :title title :tags ["apple" "reviews"] } (mgcol/find-map-by-id coll oid)))))

(deftest pop-last-values-from-multiple-arrays-using-$pop-modifier
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$pop modifier removes last or first value in the array"]
    (mgcol/insert coll { :_id oid :title title :tags ["products" "apple" "reviews"] :categories ["apple" "reviews" "drafts"] })
    (mgcol/update coll { :_id oid } { $pop { :tags 1 :categories 1 } })
    (is (= { :_id oid :title title :tags ["products" "apple"] :categories ["apple" "reviews"] } (mgcol/find-map-by-id coll oid)))))


;;
;; $pull
;;

(deftest remove-all-value-entries-from-array-using-$pull-modifier
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$pull modifier removes all value entries in the array"]
    (mgcol/insert coll { :_id oid :title title :measurements [1.0 1.2 1.2 1.2 1.1 1.1 1.2 1.3 1.0] })
    (mgcol/update coll { :_id oid } { $pull { :measurements 1.2 } })
    (is (= { :_id oid :title title :measurements [1.0 1.1 1.1 1.3 1.0] } (mgcol/find-map-by-id coll oid)))))

(deftest remove-all-value-entries-from-array-using-$pull-modifier-based-on-a-condition
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$pull modifier removes all value entries in the array"]
    (mgcol/insert coll { :_id oid :title title :measurements [1.0 1.2 1.2 1.2 1.1 1.1 1.2 1.3 1.0] })
    (mgcol/update coll { :_id oid } { $pull { :measurements { $gte 1.2 } } })
    (is (= { :_id oid :title title :measurements [1.0 1.1 1.1 1.0] } (mgcol/find-map-by-id coll oid)))))
;;
;; $pullAll
;;

(deftest remove-all-value-entries-from-array-using-$pullAll-modifier
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$pullAll modifier removes entries of multiple values in the array"]
    (mgcol/insert coll { :_id oid :title title :measurements [1.0 1.2 1.2 1.2 1.1 1.1 1.2 1.3 1.0] })
    (mgcol/update coll { :_id oid } { $pullAll { :measurements [1.0 1.1 1.2] } })
    (is (= { :_id oid :title title :measurements [1.3] } (mgcol/find-map-by-id coll oid)))))


;;
;; $rename
;;

(deftest rename-a-single-field-using-$rename-modifier
  (let [coll   "docs"
        oid    (ObjectId.)
        title "$rename renames fields"
        v     [1.0 1.2 1.2 1.2 1.1 1.1 1.2 1.3 1.0]]
    (mgcol/insert coll { :_id oid :title title :measurements v })
    (mgcol/update coll { :_id oid } { $rename { :measurements "results" } })
    (is (= { :_id oid :title title :results v } (mgcol/find-map-by-id coll oid)))))
