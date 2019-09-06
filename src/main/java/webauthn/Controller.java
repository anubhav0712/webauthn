package webauthn;

import java.io.IOException;
import java.net.CacheRequest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

import org.apache.el.stream.Optional;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
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
		    .id("herukoapp.com")
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
		private static Logger logger = LoggerFactory.getLogger(Controller.class);
	
	@CrossOrigin	
	@RequestMapping("/index")
	public String index() {
		return "Welcome ";
	}
	
	@CrossOrigin
	@RequestMapping("/startRegistration")
	public String startRegistration() {
		System.out.println("got a call");
		SecureRandom random = new SecureRandom();
		byte[] userHandle = new byte[10];
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
	
	@CrossOrigin
	@RequestMapping(value="/finishRegistration" ,method = RequestMethod.POST)
	public String finishRegistration(org.springframework.http.HttpEntity<String> httpRequest) {
		String response=null;
		String responseJson = httpRequest.getBody();
		System.out.println("header"+httpRequest.getHeaders());
		System.out.println("responseJSon "+responseJson);
		PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc=null;
		ObjectMapper mapper = new ObjectMapper()
			    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			    .setSerializationInclusion(Include.NON_ABSENT)
			    .registerModule(new Jdk8Module());
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
				response=e.getLocalizedMessage();
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.out.println("Something more fishy.....");
			response = e.getLocalizedMessage();
			e.printStackTrace();
		}
		if(response==null) {
			response="OK registered!";
		}
		logger.info("hehhhahahhahahaa done");
		logger.debug("Debug log message");
        logger.info("Info log message");
        logger.error("Error log message");
		return response+" :: jsonfromClient == "+responseJson;
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
	
	@RequestMapping(value="/finishAuthentication",method = RequestMethod.POST, consumes = org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String finishAuthentication(org.springframework.http.HttpEntity<String> httpRequest) {
		String response=null;
		String responseJson = httpRequest.getBody();
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
			        return result.getUsername()+":loggedin";
			    }
			} catch (AssertionFailedException e) { /* ... */ }
			throw new RuntimeException("Authentication failed");
	}
}
