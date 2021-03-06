(ns hanami.basic-charts

  (:require [clojure.string :as str]
            #_[clojure.data.csv :as csv]
            [clojure.data.json :as json]

            #_[aerial.utils.math.probs-stats :as p]
            #_[aerial.utils.math.infoth :as it]

            #_[aerial.utils.string :as str]
            #_[aerial.utils.coll :as coll]

            [aerial.hanami.common :as hc :refer [RMV]]
            [aerial.hanami.templates :as ht]
            [aerial.hanami.core :as hmi]))



(defn log2 [x]
  (let [ln2 (Math/log 2)]
    (/ (Math/log x) ln2)))

(defn roundit [r & {:keys [places] :or {places 4}}]
  (let [n (Math/pow 10.0 places)]
    (-> r (* n) Math/round (/ n))))


(defn connfn [data]
  (assoc data :exps ["eid1" "eid2" "eid3"]))

(hmi/start-server
 3003
 :route-handler (hmi/hanami-handler
                 (hmi/hanami-routes :index-path "public/Fig/index.html"))
 :idfn (constantly "Exploring")
 :connfn connfn)
#_(hmi/stop-server)


(hc/update-defaults
 :USERDATA {:tab {:id :TID, :label :TLBL, :opts :TOPTS}
            :opts :OPTS
            :vid :VID, :msgop :MSGOP, :session-name :SESSION-NAME}
 :VID hc/RMV, :MSGOP :tabs, :SESSION-NAME "Exploring"
 :TID :expl1, :TLBL #(-> :TID % name str/capitalize)
 :OPTS (hc/default-opts :vgl), :TOPTS (hc/default-opts :tab))

(hc/xform (hc/get-default :USERDATA) :TID :geo :VID :v1)



;;; Simple scatter with template
(->> (hc/xform ht/point-chart
       ;;:DATA (->> "http://localhost:3003/data/cars.json" slurp json/read-str)
       :UDATA "data/cars.json"
       :X "Horsepower" :Y "Miles_per_Gallon" :COLOR "Origin")
     hmi/sv!)

;;; Same as above but 'template' == base vega-lite spec 
(->>
 (hc/xform
  {:usermeta :USERDATA
   :data {:url "data/cars.json"},
   :mark "point",
   :encoding {:x {:field "Horsepower", :type "quantitative"},
              :y {:field "Miles_per_Gallon", :type "quantitative"},
              :color {:field "Origin", :type "nominal"}}})
 hmi/sv!)


;;; with framing
(let [text "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quod si ita est, sequitur id ipsum, quod te velle video, omnes semper beatos esse sapientes. Tamen a proposito, inquam, aberramus."
      frame {:frame
             {:top `[[gap :size "150px"]
                     [p "An example showing a "
                      [:span.bold "picture "] [:span.italic.bold "frame"]
                      ". This is the top 'board'"
                      [:br] ~text]]
              :left `[[gap :size "10px"]
                      [p {:style {:width "100px" :min-width "50px"}}
                       "Some text on the " [:span.bold "left:"] [:br] ~text]]
              :right `[[gap :size "2px"]
                       [p {:style {:width "200px" :min-width "50px"
                                   :font-size "20px" :color "red"}}
                        "Some large text on the " [:span.bold "right:"] [:br]
                        ~(.substring text 0 180)]]
              :bottom `[[gap :size "200px"]
                        [title :level :level3
                         :label [p {:style {:font-size "large"}}
                                 "Some text on the "
                                 [:span.bold "bottom"] [:br]
                                 "With a cool info button "
                                 [info-button
                                  :info
                                  [:p "Check out Saite Visualizer!" [:br]
                                   "Built with Hanami!" [:br]
                                   [hyperlink-href
                                    :label "Saite "
                                    :href  "https://github.com/jsa-aerial/saite"
                                    :target "_blank"]]]]]]}}]
  (->> [(hc/xform ht/point-chart
          :USERDATA
          (merge
           (hc/get-default :USERDATA) frame)
          :UDATA "data/cars.json"
          :X "Horsepower" :Y "Miles_per_Gallon" :COLOR "Origin")
        #_(hc/xform ht/point-chart
          :USERDATA
          (merge
           (hc/get-default :USERDATA) frame)
          :UDATA "data/cars.json"
          :X "Horsepower" :Y "Miles_per_Gallon" :COLOR "Origin")]
       hmi/sv!))


;;; Simple Barchart with instrumented template
(->>
 (let [data [{:a "A", :b 28 },
             {:a "B", :b 55 },
             {:a "C", :b 43 },
             {:a "D", :b 91 },
             {:a "E", :b 81 },
             {:a "F", :b 53 },
             {:a "G", :b 19 },
             {:a "H", :b 87 },
             {:a "I", :b 52 }]
       min -10.0
       minstr (-> min str (str/split #"\.") first)
       max 10.0
       maxstr (-> max str (str/split #"\.") first (#(str "+" %)))]
   (hc/xform ht/bar-chart
             :USERDATA
             (merge
              (hc/get-default :USERDATA)
              {:vid :bc1
               :slider `[[gap :size "10px"] [label :label "Add Bar"]
                         [label :label ~minstr]
                         [slider
                          :model :m1
                          :min ~min, :max ~max, :step 1.0
                          :width "200px"
                          :on-change :oc1]
                         [label :label ~maxstr]
                         [input-text
                          :model :m1
                          :width "60px", :height "26px"
                          :on-change :oc2]]})
             :HEIGHT 300, :WIDTH 350
             :X "a" :XTYPE "ordinal" :XTITLE "Foo" :Y "b" :YTITLE "Bar"
             :DATA data))
 hmi/sv!)

(hmi/sd! {:usermeta {:msgop :data :vid :bc1}
          :data (mapv #(assoc % :b (+ 50 (% :b)))
                      [{:a "A", :b 28 },{:a "B", :b 55 },{:a "C", :b 43 },
                       {:a "D", :b 91 },{:a "E", :b 81 },{:a "F", :b 53 },
                       {:a "G", :b 19 },{:a "H", :b 87 },{:a "I", :b 52 }])})



(->>
 (hc/xform
  {:usermeta :USERDATA
   :height 600,
   :width 600,
   :data
   {:values
    [{:dose 0.5, :response 32659}
     {:dose 0.5, :response 40659}
     {:dose 0.5, :response 29000}
     {:dose 1, :response 31781}
     {:dose 1, :response 30781}
     {:dose 1, :response 35781}
     {:dose 2, :response 30054}
     {:dose 4, :response 29398}
     {:dose 5, :response 27779}
     {:dose 10, :response 27915}
     {:dose 15, :response 27410}
     {:dose 20, :response 25819}
     {:dose 50, :response 23999}
     {:dose 50, :response 25999}
     {:dose 50, :response 20999}]},
   :layer
   [{:selection {:grid {:type "interval", :bind "scales"}},
     :mark {:type "point", :filled true, :color "black"},
     :encoding
     {:x {:field "dose", :type "quantitative", :scale {:type "log"}},
      :y {:field "response", :type "quantitative", :aggregate "mean"}}}
    {:mark {:type "errorbar", :ticks true},
       :encoding
       {:x {:field "dose", :type "quantitative", :scale {:zero false}},
        :y {:field "response", :type "quantitative"},
        :color {:value "#4682b4"}}}]})
 hmi/sv!)

(->>
 (hc/xform
  {:usermeta :USERDATA
   :height 600,
   :width 600,
   :data
   {:values
    [{:Dose 0.5, :Response 32659.00003,
      :drc_dose 0.05, :drc_ll3 35597.08053881955},
     {:Dose 0.5, :Response 40659.00002340234,
      :drc_dose 1, :drc_ll3 35597.08053881955},
     {:Dose 0.5, :Response 29000,
      :drc_dose 2, :drc_ll3 35597.08053881955},
     {:Dose 1, :Response 31781,
      :drc_dose 5, :drc_ll3 35597.08053881955},
     {:Dose 1, :Response 30781,
      :drc_dose 10, :drc_ll3 35597.08053881955},
     {:Dose 1, :Response 35781,
      :drc_dose 50, :drc_ll3 35597.08053881955},
     {:Dose 2, :Response 30054,
      :drc_dose 200, :drc_ll3 35597.08053881955},
     {:Dose 4, :Response 29398,
      :drc_dose 1000, :drc_ll3 35597.08053881955}]},
   :layer
   [{:selection {:grid {:type "interval", :bind "scales"}},
     :mark {:type "point", :filled true, :color "black"},
     :encoding
     {:x {:field "Dose", :type "quantitative", :scale {:type "log"}},
      :y {:field "Response", :type "quantitative", :aggregate "mean"}}}
    {:mark {:type "errorbar", :ticks true},
       :encoding
       {:x {:field "Dose", :type "quantitative", :scale {:zero false}},
        :y {:field "Response", :type "quantitative"},
        :color {:value "black"}}}
    {:mark {:type "line", :color "red"},
     :encoding
     {:x {:field "drc_dose", :type "quantitative"},
      :y {:field "drc_ll3", :type "quantitative"}}}]})
 hmi/sv!)



;;; Geo Example
(->>
 (hc/xform
  {:usermeta :USERDATA
   :width 500,
   :height 300,
   :data {:url "data/airports.csv"},
   :projection {:type "albersUsa"},
   :mark "circle",
   :encoding {:longitude {:field "longitude", :type "quantitative"},
              :latitude {:field "latitude", :type "quantitative"},
              :tooltip [{:field "name", :type "nominal"}
                        {:field "longitude", :type "quantitative"}
                        {:field "latitude", :type "quantitative"}],
              :size {:value 10}},
   :config {:view {:stroke "transparent"}}}
  :TID :geo)
 hmi/sv!)



hc/subkeyfns
(hc/update-subkeyfns :TOOLTIP hc/RMV)

(->
 (let [data (->> (range 0.005 0.999 0.001)
                 (mapv (fn[p] {:x p, :y (- (log2 p)) :col "SI"})))]
   (hc/xform ht/line-chart
             :TID :multi :TOPTS {:order :row, :size "auto"}
             :TITLE "Self Information (unexpectedness)"
             :XTITLE "Probability of event", :YTITLE "-log(p)"
             :DATA data))
 hmi/sv!)




;;; Multi Chart - cols and rows
;;;
(->>
 [(let [data (->> (range 0.005 0.999 0.001)
                  (mapv (fn[p] {:x p, :y (- (log2 p)) :col "SI"})))]
    ;; Self Info - unexpectedness
    (hc/xform ht/layer-chart
              :TID :multi :TOPTS {:order :row, :size "auto"}
              :TITLE "Self Information (unexpectedness)"
              :LAYER [(hc/xform ht/line-layer
                                :XTITLE "Probability of event"
                                :YTITLE "-log(p)")
                      (hc/xform ht/xrule-layer :AGG "mean")]
              :DATA data))
  ;; Entropy - unpredictability
  (let [data (->> (range 0.00005 0.9999 0.001)
                  (mapv (fn[p] {:x p,
                               :y (- (- (* p (log2 p)))
                                     (* (- 1 p) (log2 (- 1 p))))})))]
    (hc/xform ht/layer-chart
              :USERDATA (merge (hc/get-default :USERDATA)
                               {:test2 [{:id 1 :label "One" :val 1}
                                        {:id 2 :label "Two" :val 2}
                                        {:id 3 :label "Three" :val 3}]})
              :TID :multi
              :TITLE "Entropy (Unpredictability)"
              :LAYER [(hc/xform ht/gen-encode-layer
                                :MARK "line"
                                :XTITLE "Probability of event" :YTITLE "H(p)")
                      (hc/xform ht/xrule-layer :AGG "mean")]
              :DATA data))]
 hmi/sv!)



;;; Some distributions
;;;;
(def obsdist
  (let [obs [[0 9] [1 78] [2 305] [3 752] [4 1150] [5 1166]
             [6 899] [7 460] [8 644] [9 533] [10 504]]
        totcnt (->> obs (mapv second) (apply +))
        pdist (map (fn[[k cnt]] [k (double (/ cnt totcnt))]) obs)]
    pdist))
;;(p/mean obsdist) => 5.7
(->>
 [(hc/xform ht/layer-chart
            :TID :dists :TOPTS {:order :row, :size "auto"}
            :TITLE "A Real (obvserved) distribution with incorrect simple mean"
            :HEIGHT 400 :WIDTH 450
            :LAYER
            [(hc/xform ht/bar-layer :XTITLE "Count" :YTITLE "Probability")
             (hc/xform ht/xrule-layer :AGG "mean")]
            :DATA (mapv (fn[[x y]] {:x x :y y :m 5.7}) obsdist))

  (hc/xform ht/layer-chart
            :TID :dists
            :TITLE "The same distribution with correct weighted mean"
            :HEIGHT 400 :WIDTH 450
            :LAYER
            [(hc/xform ht/bar-layer :XTITLE "Count" :YTITLE "Probability")
             (hc/xform ht/xrule-layer :X "m")]
            :DATA (mapv (fn[[x y]] {:x x :y y :m 5.7}) obsdist))]
 hmi/sv!)



;;; Contour maps (a Vega template!)
;;;
(->>
 (hc/xform
  ht/contour-plot
  :OPTS (merge (hc/default-opts :vgl) {:mode "vega"})
  :HEIGHT 400, :WIDTH 500
  :X "Horsepower", :XTITLE "Engine Horsepower"
  :Y "Miles_per_Gallon" :YTITLE "Miles/Gallon"
  :UDATA "data/cars.json"
  :XFORM-EXPR #(let [d1 (% :X)
                     d2 (% :Y)]
                 (format "datum['%s'] != null && datum['%s'] !=null" d1 d2)))
 hmi/sv!)

(->>
 (hc/xform
  ht/contour-plot
  :HEIGHT 500, :WIDTH 600
  :OPTS (merge (hc/default-opts :vgl) {:mode "vega"})
  :DATA (take 400 (repeatedly #(do {:x (rand-int 300) :y (rand-int 50)})))
  :XFORM-EXPR #(let [d1 (% :X)
                     d2 (% :Y)]
                 (format "datum['%s'] != null && datum['%s'] !=null" d1 d2)))
 hmi/sv!)




;;; =====================================================================;;;



(->>
 (let [data (concat (->> obsdist
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Real"})))
                    (->> (p/binomial-dist 10 0.57)
                         (mapv (fn[[x y]]
                                 {:cnt x :y (roundit y)
                                  :dist "Binomial"}))))]
   (hc/xform ht/col-grouped-bar-chart
             {:WIDTH (-> 550 (/ 11) double Math/round (- 15))
              :TITLE "Real vs Binomial 0.57", :TOFFSET 40
              :DATA data
              :X "dist" :XTYPE "nominal" :XTITLE ""
              :Y "y" :YTITLE "Probability"
              :COLUMN "cnt" :COLTYPE "ordinal"}))
 (hmi/sv! "Exploring" :dists))


(->
 (let [data (concat (->> obsdist
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Real"
                                  :tt (str y)})))
                    (->> (p/binomial-dist 10 0.57)
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Binomial"
                                  :tt (str y)}))))]
   (hc/xform ht/grouped-sq-cnt-chart
             {:WIDTH (-> 550 (/ 11) double Math/round (- 15))
              :TITLE "Real vs Binomial 0.57"
              :DATA data
              :X "dist" :XTYPE "nominal" :XTITLE ""
              :Y "y" :YTITLE "Probability"
              :COLUMN "cnt" :COLTYPE "ordinal"
              }))
 hmi/sv!)



(->
 (let [data (->> "~/Bio/Rachel-Abhishek/lib-sq-counts.clj"
                 fs/fullpath slurp read-string (coll/takev 3)
                 (mapcat #(->> % (sort-by :cnt >) (coll/takev 200))) vec)]
   (hc/xform ht/grouped-sq-cnt-chart
             {:WIDTH (-> 550 (/ 9) double Math/round (- 15))
              :TITLE (format "Counts for %s"
                             (->> data first :nm (str/split #"-") first))
              :DATA data
              :X "nm" :XTYPE "nominal" :XTITLE ""
              :Y "cnt" :YTITLE "Count"
              :COLUMN "sq" :COLTYPE "nominal"
              }))
 hmi/sv!)







(-> {:title  {:text "Real distribution vs Binomials"
              :offset 10}
     :height 80
     :width  450
     :background "floralwhite"
     :mark "bar"
     :encoding {:x {:field "cnt"
                    :type "ordinal"
                    ;;:scale {:rangeStep 1}
                    :axis {:title ""}}
                :y {:field "y"
                    :axis {:title "Probability"}
                    :type "quantitative"}
                :row {:field "dist" :type "nominal"}
                :color {:field "dist" :type "nominal"
                        :scale {:scheme {:name "greenblue" #_"category20c"
                                         :extent [0.4 1]}}
                        }
                :tooltip {:field "tt" :type "nominal"}
                }

     :data {:values
            (concat (->> obsdist
                         (mapv (fn[[x y]]
                                 {:cnt x :y y :dist "Real"
                                  :tt (str y)})))
                    (mapcat #(let [l (hc/roundit %)]
                               (->> (p/binomial-dist 10 %)
                                    (mapv (fn[[x y]]
                                            {:cnt x :y y
                                             :dist (str l)
                                             :tt (str (hc/roundit y))}))))
                            (range 0.1 0.9 0.2)))}

     :config {:bar {:binSpacing 0
                    :discreteBandSize 1
                    :continuousBandSize 1}
              :view {:stroke "transparent"},
              :axis {:domainWidth 1}}}
    hmi/sv!)


;;; :background "beige"
;;; :background "aliceblue"
;;; :background "floralwhite" ; <-- These are
;;; :background "ivory"       ; <-- the better
;;; :background "mintcream"
;;; :background "oldlace"

(->> {:title  {:text "KLD minimum entropy: True P to Binomial Q estimate"
               :offset 5}
      :height 500
      :width  550
      :background "floralwhite"
      :layer
      [#_{:mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       ;:axis {:title "Binomial Distribution P paramter"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       ;:axis {:title "KLD(P||Q)"}
                       }
                   }}
       {:mark "circle"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}}
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "KLD(P||Q)"}}
                   :tooltip {:field "tt" :type "nominal"}
                   }}]

      :data {:values (mapv #(let [RE (it/KLD (->> obsdist (into {}))
                                            (->> (p/binomial-dist 10 %)
                                                 (into {})))
                                  REtt (hc/roundit RE)
                                  ptt (hc/roundit % :places 2)]
                              {:x % :y RE :tt (str ptt ", " REtt)})
                           (range 0.06 0.98 0.01))}
      }

     hmi/sv!)

(->> {:title  {:text "JSD minimum entropy: True P to Binomial Q estimate"
               :offset 5}
      :height 500
      :width  550
      :background "floralwhite"

      :layer
      [#_{:mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}}
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "JSD(P||Q)"}}
                   }}
       {:mark "circle"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}}
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "JSD(P||Q)"}}
                   :tooltip {:field "tt" :type "nominal"}
                   }}]

      :data {:values (mapv #(let [RE (it/jensen-shannon
                                      (->> obsdist (into {}))
                                      (->> (p/binomial-dist 10 %)
                                           (into {})))
                                  REtt (hc/roundit RE)
                                  ptt (hc/roundit % :places 2)]
                              {:x % :y RE :tt (str ptt ", " REtt)})
                           (range 0.06 0.98 0.01))}
      }

     hmi/sv!)


(->> {:title  {:text "Minimum entropy: True P to Binomial Q estimate"
               :offset 5}
      :height 500
      :width  500
      :background "floralwhite"

      :layer
      [{:transform [{:filter {:field "RE" :equal "KLD"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "KLD(P||Q)" :grid false}
                       }
                   :color {:field "RE" :type "nominal"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "RE"}}
                   }}
       {:transform [{:filter {:field "RE" :equal "JSD"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Binomial Distribution P paramter"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "JSD(P||Q)" :grid false}
                       }
                   :color {:field "RE" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "RE"}}
                   }}]
      :resolve {:y {:scale "independent"}}

      :data {:values (concat
                      (mapv #(let [RE (it/KLD (->> obsdist (into {}))
                                              (->> (p/binomial-dist 10 %)
                                                   (into {})))]
                               {:x % :y RE :RE "KLD"})
                            (range 0.06 0.98 0.01))
                      (mapv #(let [RE (it/jensen-shannon
                                       (->> obsdist (into {}))
                                       (->> (p/binomial-dist 10 %)
                                            (into {})))]
                               {:x % :y RE :RE "JSD"})
                            (range 0.06 0.98 0.01))) }
      }

     hmi/sv!)


(count panclus.clustering/ntsq)
(def credata
  (let [mx 1.0
        %same (range mx 0.05 -0.05)
        sqs (hc/mutate panclus.clustering/ntsq %same)]
    (->> (hc/opt-wz (concat [["s1" panclus.clustering/ntsq]]
                            (map #(vector (str "s" (+ 2 %1)) %2)
                                 (range) (->> sqs shuffle (take 5))))
                    :alpha "AUGC" :limit 14)
         second (apply concat)
         (reduce (fn[M v] (assoc M (first v) v)) {})
         vals
         (map (fn[[x y _ sq]] {:x x :y y :m 9.0})))))



(-> {:title  {:text "CRE / Optimal Word Size"}
     :height 500
     :width 550
     :background "floralwhite"
     :layer
     [{:mark "line"
       :encoding {:x {:field "x"
                      :axis {:title "Word Size"}
                      :type "quantitative"}
                  :y {:field "y"
                      :axis {:title "CRE"}
                      :type "quantitative"}
                  }}
      {:mark "rule"
       :encoding {:x {:field "m"
                      :type "quantitative"}
                  :size {:value 1}
                  :color {:value "red"}}}]
     :data {:values (conj credata
                          {:x 1 :y 1.1 :m 9.0} {:x 2 :y 1.63 :m 9.0})}}
    hmi/sv!)









;;; Lowess charting....
;;;
(def base-xy
  (->> "/home/jsa/Bio/no-lowess.clj" slurp read-string))
(def lowess-1
  (->> "/home/jsa/Bio/1-lowess.clj" slurp read-string))
(def lowess-2
  (->> "/home/jsa/Bio/2-lowess.clj" slurp read-string))
(def lowess-3
  (->> "/home/jsa/Bio/3-lowess.clj" slurp read-string))
(def lowess-4
  (->> "/home/jsa/Bio/4-lowess.clj" slurp read-string))


(->> {:title  {:text "Raw vs 1-4 lowess smoothing"
               :offset 5}
      :height 500
      :width  700
      :background "floralwhite"

      :data {:values (concat base-xy lowess-1 lowess-2 lowess-3 lowess-4) }

      :layer
      [{:transform [{:filter {:field "NM" :equal "NoL"}}]
        :mark "circle"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L1"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L2"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L3"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}
       {:transform [{:filter {:field "NM" :equal "L4"}}]
        :mark "line"
        :encoding {:x {:field "x"
                       :type "quantitative"
                       :axis {:title "Position"}
                       }
                   :y {:field "y"
                       :type "quantitative"
                       :axis {:title "Count"}
                       }
                   :color {:field "NM" :type "nominal"
                           :value "SeaGreen"
                           :legend {:type "symbol"
                                    :offset 0
                                    :title "NM"}}
                   }}]
      ;;:resolve {:y {:scale "independent"}}

      }

     hmi/sv!)
