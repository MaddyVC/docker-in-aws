(ns swarmpit.component.secret.create
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.editor :as editor]
            [swarmpit.component.state :as state]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.ajax :as ajax]
            [swarmpit.routes :as routes]
            [sablono.core :refer-macros [html]]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]))

(enable-console-print!)

(def editor-id "secret-editor")

(defn- form-name [value]
  (form/comp
    "NAME"
    (comp/vtext-field
      {:name     "name"
       :key      "name"
       :required true
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:secretName] v state/form-value-cursor))})))

(defn- form-data [value]
  (comp/vtext-field
    {:id            editor-id
     :name          "secret-editor"
     :key           "secret-editor"
     :multiLine     true
     :rows          10
     :rowsMax       10
     :value         value
     :underlineShow false
     :fullWidth     true}))

(defn- create-secret-handler
  []
  (ajax/post
    (routes/path-for-backend :secret-create)
    {:params     (state/get-value state/form-value-cursor)
     :state      [:processing?]
     :on-success (fn [{:keys [response origin?]}]
                   (when origin?
                     (dispatch!
                       (routes/path-for-frontend :secret-info (select-keys response [:id]))))
                   (message/info
                     (str "Secret " (:id response) " has been created.")))
     :on-error   (fn [{:keys [response]}]
                   (message/error
                     (str "Secret creation failed. " (:error response))))}))

(defn- init-form-state
  []
  (state/set-value {:valid?      false
                    :processing? false} state/form-state-cursor))

(defn- init-form-value
  []
  (state/set-value {:secretName nil
                    :data       ""} state/form-value-cursor))

(def mixin-init-form
  (mixin/init-form
    (fn [_]
      (init-form-state)
      (init-form-value))))

(def mixin-init-editor
  {:did-mount
   (fn [state]
     (let [editor (editor/default editor-id)]
       (.on editor "change" (fn [cm] (state/update-value [:data] (-> cm .getValue) state/form-value-cursor))))
     state)})

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin-init-editor [_]
  (let [{:keys [secretName data encode]} (state/react state/form-value-cursor)
        {:keys [valid? processing?]} (state/react state/form-state-cursor)]
    [:div
     [:div.form-panel
      [:div.form-panel-left
       (panel/info icon/secrets "New secret")]
      [:div.form-panel-right
       (comp/progress-button
         {:label      "Create"
          :disabled   (not valid?)
          :primary    true
          :onTouchTap create-secret-handler} processing?)]]
     [:div.form-edit
      (form/form
        {:onValid   #(state/update-value [:valid?] true state/form-state-cursor)
         :onInvalid #(state/update-value [:valid?] false state/form-state-cursor)}
        (form-name secretName)
        (form-data data))]]))
