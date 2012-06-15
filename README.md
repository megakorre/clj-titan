# clj-titan

A thin wrapper for the titan libary [https://github.com/thinkaurelius/titan/wiki]
this libary dosent wrapp the data types it only gives you some
nicer functions and macros that can use keywords and maps

```clojure

;; !!!!! example !!!!!!

;; include the libary
(use 'clj-titan.core)

(def graph (open "/tmp/titan-data"))

;; create some data
(with-graph graph

 (let [
 
   ;; a person 
   nils (vertex { :name "Nils" :email "some@something" })
   bert (vertex { :name "Bert" :email "bert@something" })

   bossy (vertex { :name "BossyBoss" :email "theboss@something" })]
   
   ;; create some relationshipp's
   
   (edge nils bossy :boss {})
   (edge bert bossy :boss {})

   (edge nils bert :freands {})))

;; print out a some clojure data

(use 'clojure.pprint)

(pprint
 (map #(data % 5) ;; 5 is the depth of the tree to print
   (all-vertices graph))) ;; can also use the with-graph macro

;; more functions are

(edges vert) ;; all edges of a vertex
(id vert) ;; gets the id of a element
(from-direction edge :in) ;; gets you the vertex in the direction :out /
:in
(fields element) ;; a map of the fields in the element
(with-transaction
 ~@code) ;; run code in a transaction rollback if exception
 
(other-end edge vert) ;; given a edge and a vertex gives you the
vertex att the other side



```

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
