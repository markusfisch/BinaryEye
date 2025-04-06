package de.markusfisch.android.binaryeye.content

import android.content.Context
import de.markusfisch.android.binaryeye.R

private val idToLabel = hashMapOf(
	"DCA" to R.string.idl_vehicle_class,
	"DCB" to R.string.idl_restriction_codes,
	"DCD" to R.string.idl_endorsement_codes,
	"DBA" to R.string.idl_date_of_expiry,
	"DCS" to R.string.idl_customer_family_name,
	"DAC" to R.string.idl_customer_first_name,
	"DCT" to R.string.idl_customer_first_name_alt,
	"DAD" to R.string.idl_customer_middle_name,
	"DBD" to R.string.idl_date_of_issue,
	"DBB" to R.string.idl_date_of_birth,
	"DBC" to R.string.idl_sex,
	"DAY" to R.string.idl_eye_color,
	"DAU" to R.string.idl_body_height,
	"DAG" to R.string.idl_address_street,
	"DAI" to R.string.idl_address_city,
	"DAJ" to R.string.idl_address_jurisdiction_code,
	"DAK" to R.string.idl_address_postal_code,
	"DAQ" to R.string.idl_customer_id_number,
	"DCF" to R.string.idl_document_discriminator,
	"DCG" to R.string.idl_country,
	"DDE" to R.string.idl_family_name_truncation,
	"DDF" to R.string.idl_first_name_truncation,
	"DDG" to R.string.idl_middle_name_truncation,
	"DAH" to R.string.idl_address_street_2,
	"DAZ" to R.string.idl_hair_color,
	"DCI" to R.string.idl_place_of_birth,
	"DCJ" to R.string.idl_audit_information,
	"DCK" to R.string.idl_inventory_control_number,
	"DBN" to R.string.idl_alias_family_name,
	"DBG" to R.string.idl_alias_given_name,
	"DBS" to R.string.idl_alias_suffix_name,
	"DCU" to R.string.idl_name_suffix,
	"DCE" to R.string.idl_weight_range,
	"DCL" to R.string.idl_ethnicity,
	"DCM" to R.string.idl_standard_vehicle_classification,
	"DCN" to R.string.idl_standard_endorsement_code,
	"DCO" to R.string.idl_standard_restriction_code,
	"DCP" to R.string.idl_vehicle_classification_description,
	"DCQ" to R.string.idl_endorsement_code_description,
	"DCR" to R.string.idl_restriction_code_description,
	"DDA" to R.string.idl_compliance_type,
	"DDB" to R.string.idl_card_revision_date,
	"DDC" to R.string.idl_hazmat_endorsement_expiration_date,
	"DDD" to R.string.idl_limited_duration_document_indicator,
	"DAW" to R.string.idl_weight_pounds,
	"DAX" to R.string.idl_weight_kilograms,
	"DDH" to R.string.idl_under_18_until_date,
	"DDI" to R.string.idl_under_19_until_date,
	"DDJ" to R.string.idl_under_21_until_date,
	"DDK" to R.string.idl_organ_donor_indicator,
	"DDL" to R.string.idl_veteran_indicator
)

fun Context.idlToRes(id: String): String {
	val resId = idToLabel[id]
	return if (resId != null) {
		getString(resId)
	} else {
		id
	}
}
