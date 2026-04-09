package de.markusfisch.android.binaryeye.content

import android.content.Context
import de.markusfisch.android.binaryeye.R
import de.tsenger.vdstools.DataEncoder
import de.tsenger.vdstools.Verifier
import de.tsenger.vdstools.idb.IdbSeal
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.icao.CscaMasterList
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x9.ECNamedCurveTable
import org.bouncycastle.asn1.x9.X962Parameters
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Provider
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

object IdbVerifier {
	private const val MASTER_LIST_ASSET = "DE_ML_2026-01-08-12-20-54.ml"

	private val provider: Provider by lazy {
		BouncyCastleProvider().also { bcProvider ->
			if (Security.getProvider(bcProvider.name) == null) {
				Security.addProvider(bcProvider)
			}
		}
	}

	@Volatile
	private var cachedMasterListCertificates: List<X509Certificate>? = null

	fun verify(context: Context, seal: IdbSeal): List<SealField> {
		val signatureInfo = seal.signatureInfo
			?: return listOf(
				SealField(
					R.string.idb_verification,
					context.getString(R.string.idb_verification_unsigned)
				)
			)
		val headerCertificateReference = seal.signerCertReference
			?.hexToByteArrayOrNull()
		val signerCertificate = context.findCertificateByReference(
			headerCertificateReference
		)
		val curveName = signerCertificate?.let {
			curveNameFrom(it)
		}
		val signedBytes = seal.signedBytes
		val signatureResult = if (
			signerCertificate == null ||
			curveName.isNullOrBlank() ||
			signedBytes == null
		) {
			Verifier.Result.VerifyError
		} else {
			Verifier(
				signedBytes,
				signatureInfo.plainSignatureBytes,
				signerCertificate.publicKey.encoded,
				curveName
			).verify()
		}
		val referenceMatches = signerCertificate != null &&
				headerCertificateReference != null &&
				DataEncoder.buildCertificateReference(signerCertificate.encoded)
					.contentEquals(headerCertificateReference)
		val certificateValidNow = signerCertificate != null && runCatching {
			signerCertificate.checkValidity(Date())
			true
		}.getOrElse {
			false
		}
		val verification = context.getString(
			when {
				signerCertificate == null ->
					R.string.idb_verification_signer_certificate_issuer_not_found_in_master_list

				!referenceMatches ->
					R.string.idb_verification_signature_invalid

				!certificateValidNow ->
					R.string.idb_verification_signature_invalid

				signatureResult == Verifier.Result.SignatureValid ->
					R.string.idb_verification_verified

				signatureResult == Verifier.Result.SignatureInvalid ->
					R.string.idb_verification_signature_invalid

				else -> R.string.idb_verification_error
			}
		)

		val fields = mutableListOf(
			SealField(R.string.idb_verification, verification),
			SealField(
				R.string.idb_certificate_reference,
				signatureInfo.signerCertificateReference
			),
			SealField(
				R.string.vds_signing_date,
				signatureInfo.signingDate.toString()
			),
			SealField(
				R.string.idb_signature_verification,
				signatureResult.name
			),
		)
		if (signerCertificate != null) {
			fields.add(
				SealField(
					R.string.idb_signer_certificate_subject,
					signerCertificate.subjectX500Principal.name
				)
			)
			fields.add(
				SealField(
					R.string.idb_signer_certificate_issuer,
					signerCertificate.issuerX500Principal.name
				)
			)
		}
		return fields
	}

	private fun String.hexToByteArrayOrNull(): ByteArray? {
		if (length % 2 != 0) {
			return null
		}
		return runCatching {
			ByteArray(length / 2) { index ->
				substring(index * 2, index * 2 + 2).toInt(16).toByte()
			}
		}.getOrNull()
	}

	private fun Context.loadMasterListCertificates(): List<X509Certificate> {
		cachedMasterListCertificates?.let {
			return it
		}
		synchronized(this) {
			cachedMasterListCertificates?.let {
				return it
			}
			val cmsSignedData = assets.open(MASTER_LIST_ASSET).use { input ->
				CMSSignedData(input.readBytes())
			}
			val signedContent = cmsSignedData.signedContent?.content as? ByteArray
				?: throw IllegalArgumentException("Master list does not contain signed content")
			val masterList = CscaMasterList.getInstance(signedContent)
			val certificates = masterList.certStructs.map { cert ->
				JcaX509CertificateConverter()
					.setProvider(provider)
					.getCertificate(X509CertificateHolder(cert))
			}
			cachedMasterListCertificates = certificates
			return certificates
		}
	}

	private fun Context.findCertificateByReference(
		certificateReference: ByteArray?
	): X509Certificate? {
		if (certificateReference == null || certificateReference.size != 5) {
			return null
		}
		return runCatching {
			loadMasterListCertificates().firstOrNull { certificate ->
				DataEncoder.buildCertificateReference(certificate.encoded)
					.contentEquals(certificateReference)
			}
		}.getOrNull()
	}

	private fun curveNameFrom(certificate: X509Certificate): String? {
		return runCatching {
			val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(
				certificate.publicKey.encoded
			)
			val params = X962Parameters.getInstance(subjectPublicKeyInfo.algorithm.parameters)
			val oid = params.parameters as? ASN1ObjectIdentifier
			oid?.let {
				ECNamedCurveTable.getName(it)
			}
		}.getOrNull()
	}
}
