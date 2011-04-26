(ns rhq.plugin-container
  (:require [clojure.java.io :as io])
  (:import [org.rhq.core.pc PluginContainer PluginContainerConfiguration]
	   [org.rhq.core.pc.plugin FileSystemPluginFinder]
	   [org.rhq.core.pc.operation OperationContextImpl]
	   [org.rhq.core.pluginapi.operation OperationServices]
	   [org.rhq.core.domain.resource Resource ResourceType]))

(defn- create-pc-config [plugins-dir data-dir]
  (doto (PluginContainerConfiguration.)
    (.setPluginDirectory plugins-dir)
    (.setDataDirectory data-dir)
    (.setInsideAgent false)
    (.setPluginFinder (FileSystemPluginFinder. plugins-dir))
    (.setRootPluginClassLoaderRegex
     (PluginContainerConfiguration/getDefaultClassLoaderFilter))
    (.setCreateResourceClassloaders true)))

(defn- pc [] (PluginContainer/getInstance))

(defn inventory-mgr [] (.. (pc) getInventoryManager))

(defn plugin-mgr [] (.. (pc) getPluginManager))

(defn start []
  (let [basedir (io/file "target")]
    (println (.getAbsolutePath basedir))
    (.setConfiguration
     (PluginContainer/getInstance)
     (create-pc-config (io/file basedir "plugins") (io/file basedir "data")))
    (.initialize (PluginContainer/getInstance))))

(defn stop [] (.shutdown (pc)))

(defn running? [] (.isStarted (pc)))

(defn- plugin [r] (.. r getResourceType getPlugin))

(defn- resource-category [r] (.. r getResourceType getCategory getName))

(defn availability [r]
  (keyword (.name
	    (.getAvailabilityType
	     (.getCurrentAvailability (inventory-mgr) r)))))

(defn available? [r]
  (= :UP (availability r)))

(defn discover [& category]
  (cond (= :server category) (.executeServerScanImmediately (inventory-mgr))
	(= :service category) (.executeServiceScanImmediately (inventory-mgr))
	:else [(.executeServerScanImmediately (inventory-mgr))
	       (.executeServiceScanImmediately (inventory-mgr))]))

(defn inventory-filter [opts]
  (let [filters {:plugin (fn [r] (= (plugin r) (opts :plugin)))
		 :category (fn [r] (= (resource-category r)
				      (name (opts :category))))
		 :availability (fn [r] (= (availability r)
					  (opts :availability)))
		 :type (fn [r]
			 (cond (isa? (type (opts :type)) ResourceType)
			       (= (opts :type) (.getResourceType r))
			       ; else the value of :type is assumed to be
			       ; a string
			       :else (= (opts :type)
					(.. r getResourceType getName))))}]
    (fn include-resource? [r]
      (every? (fn [f] (f r))
	      (for [[k v] opts :when (filters k)] (filters k))))))

(defn inventory
  ([] (.getPlatform (inventory-mgr)))
  ([opts]
     (filter (inventory-filter opts)
	     (tree-seq #(seq (.getChildResources %))
		       #(.getChildResources %) (inventory)))))

(defn operations [r] (.. r getResourceType getOperationDefinitions))

(defmulti get-rid type)

(defmethod get-rid Resource [r] (.getId r))

(defmethod get-rid Number [r] (int r))

(defn operation [opts]
  (fn invoke-operation [& fn-opts]
    (let [new-opts (into opts fn-opts)
	  rid (get-rid (new-opts :resource))
	  op-name (new-opts :name)
	  args (new-opts :args)
	  timeout (new-opts :timeout 3000)
	  context (OperationContextImpl. rid)]
      (.invokeOperation
       (.getOperationServices context) context op-name args timeout))))

; (defn test [preds] (fn [val] (every? (fn [f] (f val)) preds)))