(defproject clj-titan "0.1.0-SNAPSHOT"

  :repositories {"sonatype-snapshots" "http://oss.sonatype.org/content/repositories/snapshots"
                 "sonatype-releases" "http://oss.sonatype.org/content/repositories/releases"} 
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  ;;; :resource-paths ["/titan-lib"]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.thinkaurelius/titan "0.1-SNAPSHOT"]])
