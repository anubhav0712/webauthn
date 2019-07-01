package webauthn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.attestation.Attestation;
import com.yubico.webauthn.data.UserIdentity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Wither;

import java.time.Instant;
import java.util.Optional;


public class CredentialRegistration {

    public long getSignatureCount() {
		return signatureCount;
	}

	public void setSignatureCount(long signatureCount) {
		this.signatureCount = signatureCount;
	}

	public UserIdentity getUserIdentity() {
		return userIdentity;
	}

	public void setUserIdentity(UserIdentity userIdentity) {
		this.userIdentity = userIdentity;
	}

	public Optional<String> getCredentialNickname() {
		return credentialNickname;
	}

	public void setCredentialNickname(Optional<String> credentialNickname) {
		this.credentialNickname = credentialNickname;
	}

	public Instant getRegistrationTime() {
		return registrationTime;
	}

	public void setRegistrationTime(Instant registrationTime) {
		this.registrationTime = registrationTime;
	}

	public RegisteredCredential getCredential() {
		return credential;
	}

	public void setCredential(RegisteredCredential credential) {
		this.credential = credential;
	}

	public Optional<Attestation> getAttestationMetadata() {
		return attestationMetadata;
	}

	public void setAttestationMetadata(Optional<Attestation> attestationMetadata) {
		this.attestationMetadata = attestationMetadata;
	}

	long signatureCount;

    UserIdentity userIdentity;
    Optional<String> credentialNickname;

    @JsonIgnore
    Instant registrationTime;
   
	RegisteredCredential credential;

    Optional<Attestation> attestationMetadata;

    @JsonProperty("registrationTime")
    public String getRegistrationTimestamp() {
        return registrationTime.toString();
    }

    public String getUsername() {
        return userIdentity.getName();
    }

}