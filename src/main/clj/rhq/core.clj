(ns rhq.core
  (:import [org.rhq.core.domain.operation OperationDefinition])
  (:import [org.rhq.core.domain.configuration.definition PropertyDefinition]))

(defprotocol Definition
  (name [this])
  (display-name [this])
  (description [this]))

(extend-protocol Definition
  OperationDefinition
  (name [def] (.getName def))
  (display-name [def] (.getDisplayName def))
  (description [def] (.getDescription def))

  PropertyDefinition
  (name [def] (.getName def))
  (display-name [def] (.getDisplayName def))
  (description [def] (.getDescription def)))