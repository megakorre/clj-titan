(ns clj-titan.core
  (:import [com.thinkaurelius.titan.core TitanFactory]))

(def ^:dynamic *graph*)

(defn open-in-mem []
  (TitanFactory/openInMemoryGraph))

(defn open [dir]
  (TitanFactory/open dir))

(defmacro with-graph [graph & code]
  `(binding [*graph* ~graph]
     ~@code))

(defmacro with-transaction [& code]
  `(try
     (do (.startTransaction *graph*)
         (let [v# (do ~@code)]
           (.stopTransaction *graph* com.tinkerpop.blueprints.TransactionalGraph$Conclusion/SUCCESS)
           v#))
     (catch Exception e#
       (.stopTransaction
        *graph*
        com.tinkerpop.blueprints.TransactionalGraph$Conclusion/FAILURE)
       (throw e#))))

(defn- set-props [fields prop-bag]
  (loop [[[key val] & r] (seq fields)]
    (if (not key)
      prop-bag
      (do
        (.setProperty prop-bag (name key) val)
        (recur r)))))

(defn vertex
  ([fields]
     (vertex nil fields))
  ([id fields]
      (let [v (.addVertex *graph* id)]
        (set-props fields v))))

(defn edge
  ([id v1 v2 type fields]
     (set-props fields (.addEdge *graph* id v1 v2 (name type))))
  ([v1 v2 type fields]
     (edge nil v1 v2 type fields)))

(defn label [edge]
  (keyword (.getLabel edge)))

(defn fields
  "gets a map of the fields in a element
   note both vertices and edges are elements

   api-src: https://github.com/tinkerpop/blueprints/blob/master/blueprints-core/
src/main/java/com/tinkerpop/blueprints/Element.java"
  [^com.tinkerpop.blueprints.Element
   element]
  (into
   {}
   (for [key (seq (.getPropertyKeys element))]
     [(keyword key) (.getProperty element key)])))

(defn direction
  "gets the direction from a keyword arg
   dir: #{ :out :in :both }
   api-src: https://github.com/tinkerpop/blueprints/blob/master/blueprints-core/
src/main/java/com/tinkerpop/blueprints/Direction.java"
  [dir]
  (case dir
    :out  com.tinkerpop.blueprints.Direction/OUT
    :in   com.tinkerpop.blueprints.Direction/IN
    :both com.tinkerpop.blueprints.Direction/BOTH))

(defn from-direction
  "When You have a edge object you can ask for a
   vertex by the direction of the edge
   dir can be :out or :in"
  [edge dir]
  (.getVertex
   edge
   (direction dir)))

(defn id [element]
  (.getId element))

(defn edges
  "Gets all the edges from a vertex
   optional argument :dir for the direction of the edges

   api-src: https://github.com/tinkerpop/blueprints/blob/master/blueprints-core/
src/main/java/com/tinkerpop/blueprints/Vertex.java"
  [^com.tinkerpop.blueprints.Vertex
   vertex & { dir :dir }]
  (.getEdges
   vertex
   (if dir (direction dir) (direction :both))
   (make-array String 0)))

(defn other-end
  "given a edge and a vertex
   gives you the vertex att the other
   end of the edge"
  [edge vertex]
  (if (= (id vertex) (id (from-direction edge :in)))
    (from-direction edge :out)
    (from-direction edge :in)))

(defn get-vertex
  [id]
  (.getVertex *graph* id))

(defn all-vertices
  []
  (.getVerices *graph*))

(defn data-dispatch
  [thing]
  (cond
   (try (label thing) (catch Exception e nil))
   :edge
   :else :vertex))

(declare data)
(def ^:dynamic *data-depth* nil)
(def ^:dynamic *data-parent* nil)
(def ^:dynamic *data-seen* #{})

(defmulti -data
  "using a sniffing function instead of the interfaces
   in blueprint ... seams they overlapp somehowe in
   the titan implementation cant distinguis betwen them"
  data-dispatch)

(defmethod -data :edge
  [edg]
  (if (and
       *data-parent*
       *data-depth*
       (> *data-depth* 0))
    (binding [*data-depth* (dec *data-depth*)
              *data-seen*  (conj *data-seen* (id edg))]
      {:id (id edg)
       :fields (fields edg)
       :label (label edg)
       :vertex (-data (other-end edg *data-parent*))})
    {:id (id edg)
     :fields (fields edg)
     :label (label edg)}))

(defmethod -data :vertex
  [vert]
  (binding [*data-parent* vert]
    (letfn [(edgs
            [dir]
              (into []
                  (->> (map data (edges vert :dir dir))
                       (map (partial merge {:dir dir}))
                       (filter #(not (*data-seen* (:id %)))))))]
    {:id (id vert)
       :fields (fields vert)
       :edges (concat (edgs :out) (edgs :in))})))

(defn data
  "Usefull function for reading data
   in the repl"
  ([thing]
     (-data thing))
  ([thing dept]
     (binding [*data-depth* dept
               *data-seen* #{}]
       (-data thing))))

(defn- create-testing-data
  []
  (let [g (open-in-mem)]
    (with-graph g
      (let [a (vertex { :name "Patrik" :email "Patrik.karlin@gmail.com" })
            b (vertex { :name "Someone" })
            e (edge a b :freands {})
            m (vertex { :name "BossyBoss" })
            mb (edge a m :boss {})]
        [a b e m mb]))))

(defn- transaction-fail-test
  []
  (with-graph (open "/tmp/titan-data")
    (let [failed-id (atom nil)]
      (try (with-transaction
             (reset! failed-id (id (vertex { :some-data "some data" })))
             (let [b (vertex { :some-other-data "hello world"})
                   e (edge (get-vertex @failed-id) b :freands { :some-data "_______" })]
               (assert (= (id (other-end e b)) @failed-id))
               (throw (new Exception "Some Error"))))
           (catch Exception e
             (assert (= "Some Error" (.getMessage e)))
             (assert (= nil (get-vertex @failed-id))))))))

(def ^:private all-tests
  [transaction-fail-test])

(defn run-tests!
  []
  (doseq [t all-tests] (t)))