package webauthn;

import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.UserIdentity;

public class Utility {
	
	public static CredentialRegistration makeCredentialFromResult(String name , RegistrationResult result , ByteArray userHandle) throws Exception{
		RegisteredCredential rc = RegisteredCredential.builder()
									.credentialId(result.getKeyId().getId())
									.userHandle(userHandle)
									.publicKeyCose(result.getPublicKeyCose())
									.build();
		UserIdentity ui = UserIdentity.builder()
							.name(name)
							.displayName(name)
							.id(result.getKeyId().getId())
							.build();
		CredentialRegistration cr = new CredentialRegistration();
		cr.userIdentity=ui;
		cr.credential=rc;
		return cr;
	}

}
