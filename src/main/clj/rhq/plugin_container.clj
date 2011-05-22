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

(defn pc
  "Returns the PluginContainer singleton object."
  []
  (PluginContainer/getInstance))

(defn inventory-mgr [] (.. (pc) getInventoryManager))

(defn plugin-mgr [] (.. (pc) getPluginManager))

(defn start
  "Initializes the plugin container. Once initialized, plugins will be activated
   and discovery scans will begin. This function is effectively a no-op if the
   plugin container is already running."
  []
  (let [basedir (io/file "target")]
    (println (.getAbsolutePath basedir))
    (.setConfiguration
     (PluginContainer/getInstance)
     (create-pc-config (io/file basedir "plugins") (io/file basedir "data")))
    (.initialize (PluginContainer/getInstance))))

(defn stop
  "Shuts down the plugin container. This function is a no-op if the plugin
   container has already been stopped."
  []
  (.shutdown (pc)))

(defn running?
  "Returns true if the plugin container has been started and is fully
   initialized."
  []
  (.isStarted (pc)))

(defn- plugin [r] (.. r getResourceType getPlugin))

(defn resource-category
  "Returns the resource category as a keyword. Possible values are,

    :PLATFORM
    :SERVER
    :SERVICE"
  [r]
  (keyword (.. r getResourceType getCategory getName)))

(defn availability
  "Returns the availability of a resource. Possible values are,

    1) :UP
    2) :DOWN
  "
  [r]
  (keyword (.name
	    (.getAvailabilityType
	     (.getCurrentAvailability (inventory-mgr) r)))))

(defn available?
  "Returns true if a resource is available, false otherwise."
  [r]
  (= :UP (availability r)))

(defn discover
  "When invoked with no argument, this fn performs a both server and service
  scans. The inventory reports are returned as a vector where the first
  element of the vector is the report from the server scan, and the second
  element is the report from the service scan.

  When invoked with a single argument, one of two values is epxected,

    1) :server
    2) :service

  where :server causes a server scan to be run and :service causes a service
  scan to be run. Returns the inventory report. If the argument is some other
  value, then it results this fn being invoked without any arguments."
  ([] [(.executeServerScanImmediately (inventory-mgr))
       (.executeServiceScanImmediately (inventory-mgr))])
  ([category]
     (cond (= :server category) (.executeServerScanImmediately (inventory-mgr))
	   (= :service category) (.executeServiceScanImmediately
				  (inventory-mgr))
	   :else (discover))))

(defn inventory-filter [opts]
  (let [filters {:plugin (fn [r] (= (plugin r) (opts :plugin)))
		 :category (fn [r] (= (resource-category r)
				      (opts :category)))
		 :availability (fn [r] (= (availability r)
					  (opts :availability)))
		 :type (fn [r]
			 (cond (isa? (type (opts :type)) ResourceType)
			       (= (opts :type) (.getResourceType r))
			       ; else the value of :type is assumed to be
			       ; a string
			       :else (= (opts :type)
					(.. r getResourceType getName))))
		 :fn (fn [r] (true? ((opts :fn) r)))}]
    (fn include-resource? [r]
      (every? (fn [f] (f r))
	      (for [[k v] opts :when (filters k)] (filters k))))))

(defn inventory
  "Returns the platform resource object when invoked with no arguments.

  Returns a lazy sequence of resources when invoked with a map of one or more
  filter options. The supported keys for the filter options map are,

    :plugin      
    :category 
    :avilability
    :type

  :plugin
  Expects the plugin name as a string

  :category
  Epxects one of three values - :PLATFORM, :SERVER, or :SERVICE

  :availability
  Expects either :UP or :DOWN

  :type
  Expected either a ResourceType object, or the resource type name as a string"
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

; (defn test [preds] (fn [val] (every? (fn [f] (f val)) preds)))(ns rhq.plugin-cdontainer)