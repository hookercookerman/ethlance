(ns ethlance.pages.invoice-create-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [ethlance.ethlance-db :as ethlance-db]
    [cljs-time.coerce :as coerce]))

(defn dispatch-contracts-load [user-id]
  (dispatch [:form/set-value :form.invoice/add-invoice :invoice/contract 0 false])
  (dispatch [:after-eth-contracts-loaded
             [:list/load-ids {:list-key :list/freelancer-my-open-contracts
                              :fn-key :ethlance-views/get-freelancer-contracts
                              :load-dispatch-key :contract.db/load-contracts
                              :fields #{:contract/job}
                              :args {:user/id user-id :contract/status 3 :job/status 0}}]]))

(defn add-invoice-form []
  (let [contracts-list (subscribe [:list/contracts :list/freelancer-my-open-contracts])
        form (subscribe [:form.invoice/add-invoice])]
    (fn []
      (let [{:keys [:data :loading? :errors]} @form
            {:keys [:invoice/contract :invoice/description :invoice/amount :invoice/worked-hours :invoice/worked-from
                    :invoice/worked-to]} data
            contracts (:items @contracts-list)]
        [paper
         {:loading? loading?}
         [:h2 "New Invoice"]
         [:div
          [ui/select-field
           {:floating-label-text "Job"
            :value (when (pos? contract) contract)
            :auto-width true
            :style styles/overflow-ellipsis
            :disabled (empty? contracts)
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/contract %3])}
           (for [{:keys [:contract/id :contract/job]} contracts]
             [ui/menu-item
              {:value id
               :primary-text (gstring/format "%s (#%s)" (:job/title job) (:job/id job))
               :key id}])]]
         [:div
          [misc/ether-field
           {:floating-label-text "Amount (Ether)"
            :value amount
            :form-key :form.invoice/add-invoice
            :field-key :invoice/amount}]]
         [:div
          [ui/text-field
           {:floating-label-text "Hours Worked"
            :value worked-hours
            :type :number
            :min 0
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/worked-hours (js/parseInt %2) pos?])}]]
         [:div
          [ui/date-picker
           {:default-date (js/Date. worked-from)
            :max-date (js/Date.)
            :floating-label-text "Worked From"
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/worked-from
                                   (coerce/to-date-time %2)])}]]
         [:div
          [ui/date-picker
           {:default-date (js/Date. worked-to)
            :max-date (js/Date.)
            :floating-label-text "Worked To"
            :on-change #(dispatch [:form/set-value :form.invoice/add-invoice :invoice/worked-to
                                   (coerce/to-date-time %2)])}]]
         [misc/textarea
          {:floating-label-text "Message"
           :form-key :form.invoice/add-invoice
           :field-key :invoice/description
           :max-length-key :max-invoice-description
           :value description
           :hint-text misc/privacy-warning-hint}]
         [misc/send-button
          {:disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.invoice/add-invoice
                                     (-> data
                                       (update :invoice/worked-from u/date->sol-timestamp)
                                       (update :invoice/worked-to u/date->sol-timestamp))])}]]))))

(defn invoice-create-page []
  (fn []
    [misc/only-registered
     [misc/only-freelancer
      {:on-user-change dispatch-contracts-load}
      [center-layout
       [add-invoice-form]]]]))
