package webauthn;

import java.io.IOException;
import java.net.CacheRequest;
import java.util.HashMap;
import java.util.Random;

import org.apache.el.stream.Optional;
import org.apache.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.net.MediaType;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

@RestController
public class Controller {
	RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
		    .id("tcs.com")
		    .name("WebAuthn")
		    .build();
	MyCredentialRepository repo = new MyCredentialRepository();
		RelyingParty rp = RelyingParty.builder()
		    .identity(rpIdentity)
		    .credentialRepository(repo)
		    .build();
		
		ObjectMapper mapper = new ObjectMapper()
			    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			    .setSerializationInclusion(Include.NON_ABSENT)
			    .registerModule(new Jdk8Module());
		
		HashMap<String , PublicKeyCredentialCreationOptions> requestCache = new HashMap<String , PublicKeyCredentialCreationOptions>();
		HashMap<String , AssertionRequest> authRequestCache = new HashMap<String,AssertionRequest>();
		HashMap<String , ByteArray> handleCache = new HashMap<String , ByteArray>();
	
	@RequestMapping("/index")
	public String index() {
		return "Welcome ";
	}
	
	@RequestMapping("/startRegistration")
	public String startRegistration() {
		Random random = new Random();
		byte[] userHandle = new byte[64];
		random.nextBytes(userHandle);
		ByteArray randomHandle = new ByteArray(userHandle);
		
		PublicKeyCredentialCreationOptions request = rp.startRegistration(StartRegistrationOptions.builder()
			    .user(UserIdentity.builder()
			        .name("alice")
			        .displayName("alice")
			        .id(randomHandle)
			        .build())
			    .build());
		String str = "Nothing...";
		requestCache.put("alice", request);
		handleCache.put("alice", randomHandle);
		try {
			str = mapper.writeValueAsString(request);
		}
		catch(Exception e) {
			System.out.println("Exception --- ");
			e.printStackTrace();
		}
		return str;
	}
	
	@RequestMapping(value="/finishRegistration" , consumes = org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE)
	public void finishRegistration(org.springframework.http.HttpEntity<String> httpRequest) {
		String responseJson = httpRequest.getBody();
		PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc=null;
		try {
			pkc =mapper.readValue(responseJson, new TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs>>(){});
			
			try {
				PublicKeyCredentialCreationOptions request = requestCache.get("alice");
			    RegistrationResult result = rp.finishRegistration(FinishRegistrationOptions.builder()
			        .request(request)
			        .response(pkc)
			        .build());
			    CredentialRegistration reg = Utility.makeCredentialFromResult("alice", result, handleCache.get("alice"));
			    repo.addRegistrationByUsername("alice", reg);
			} catch (RegistrationFailedException e) { 
				System.out.println("Something fishy.....");
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.out.println("Something more fishy.....");
			e.printStackTrace();
		}
	}
	
	@RequestMapping("/startAuthentication")
	public String startAuthentication() {
		AssertionRequest request = rp.startAssertion(StartAssertionOptions.builder()
			    .username("alice")
			    .build());
		authRequestCache.put("alice", request);
			String json="nothing...";
			try {
				json = mapper.writeValueAsString(request);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return json;
	}
	
	@RequestMapping("/finishAuthentication")
	public String finishAuthentication() {
		String responseJson = "";
		PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc=null;
		try {
			pkc = mapper.readValue(responseJson, new TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>>() {
});
		} catch(Exception e) {
			
		}

			try {
			    AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder()
			        .request(authRequestCache.get("alice"))
			        .response(pkc)
			        .build());

			    if (result.isSuccess()) {
			        return result.getUsername();
			    }
			} catch (AssertionFailedException e) { /* ... */ }
			throw new RuntimeException("Authentication failed");
	}
}
