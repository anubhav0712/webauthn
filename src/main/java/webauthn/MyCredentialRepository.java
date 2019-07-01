package webauthn;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yubico.internal.util.CollectionUtil;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class MyCredentialRepository implements RegistrationStorage , CredentialRepository{
	 private final Cache<String, Set<CredentialRegistration>> storage = CacheBuilder.newBuilder()
		        .maximumSize(1000)
		        .expireAfterAccess(1, TimeUnit.DAYS)
		        .build();


		    public boolean addRegistrationByUsername(String username, CredentialRegistration reg) {
		        try {
		            return storage.get(username, HashSet::new).add(reg);
		        } catch (ExecutionException e) {
		            throw new RuntimeException(e);
		        }
		    }

		    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
		        return getRegistrationsByUsername(username).stream()
		            .map(registration -> PublicKeyCredentialDescriptor.builder()
		                .id(registration.getCredential().getCredentialId())
		                .build())
		            .collect(Collectors.toSet());
		    }

		    @Override
		    public Collection<CredentialRegistration> getRegistrationsByUsername(String username) {
		        try {
		            return storage.get(username, HashSet::new);
		        } catch (ExecutionException e) {
		            throw new RuntimeException(e);
		        }
		    }

		    @Override
		    public Collection<CredentialRegistration> getRegistrationsByUserHandle(ByteArray userHandle) {
		        return storage.asMap().values().stream()
		            .flatMap(Collection::stream)
		            .filter(credentialRegistration ->
		                userHandle.equals(credentialRegistration.getUserIdentity().getId())
		            )
		            .collect(Collectors.toList());
		    }

		    @Override
		    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
		        return getRegistrationsByUserHandle(userHandle).stream()
		            .findAny()
		            .map(CredentialRegistration::getUsername);
		    }

		    @Override
		    public Optional<ByteArray> getUserHandleForUsername(String username) {
		        return getRegistrationsByUsername(username).stream()
		            .findAny()
		            .map(reg -> reg.getUserIdentity().getId());
		    }

		    @Override
		    public void updateSignatureCount(AssertionResult result) {
		        CredentialRegistration registration = getRegistrationByUsernameAndCredentialId(result.getUsername(), result.getCredentialId())
		            .orElseThrow(() -> new NoSuchElementException(String.format(
		                "Credential \"%s\" is not registered to user \"%s\"",
		                result.getCredentialId(), result.getUsername()
		            )));

		        Set<CredentialRegistration> regs = storage.getIfPresent(result.getUsername());
		        regs.remove(registration);
		       // regs.add(registration.withSignatureCount(result.getSignatureCount()));
		    }

		    @Override
		    public Optional<CredentialRegistration> getRegistrationByUsernameAndCredentialId(String username, ByteArray id) {
		        try {
		            return storage.get(username, HashSet::new).stream()
		                .filter(credReg -> id.equals(credReg.getCredential().getCredentialId()))
		                .findFirst();
		        } catch (ExecutionException e) {
		            throw new RuntimeException(e);
		        }
		    }

		    @Override
		    public boolean removeRegistrationByUsername(String username, CredentialRegistration credentialRegistration) {
		        try {
		            return storage.get(username, HashSet::new).remove(credentialRegistration);
		        } catch (ExecutionException e) {
		            throw new RuntimeException(e);
		        }
		    }

		    @Override
		    public boolean removeAllRegistrations(String username) {
		        storage.invalidate(username);
		        return true;
		    }

		    @Override
		    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
		        Optional<CredentialRegistration> registrationMaybe = storage.asMap().values().stream()
		            .flatMap(Collection::stream)
		            .filter(credReg -> credentialId.equals(credReg.getCredential().getCredentialId()))
		            .findAny();
		        return registrationMaybe.flatMap(registration ->
		            Optional.of(
		                RegisteredCredential.builder()
		                    .credentialId(registration.getCredential().getCredentialId())
		                    .userHandle(registration.getUserIdentity().getId())
		                    .publicKeyCose(registration.getCredential().getPublicKeyCose())
		                    .signatureCount(registration.getSignatureCount())
		                    .build()
		            )
		        );
		    }

		    @Override
		    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
		        return CollectionUtil.immutableSet(
		            storage.asMap().values().stream()
		                .flatMap(Collection::stream)
		                .filter(reg -> reg.getCredential().getCredentialId().equals(credentialId))
		                .map(reg -> RegisteredCredential.builder()
		                    .credentialId(reg.getCredential().getCredentialId())
		                    .userHandle(reg.getUserIdentity().getId())
		                    .publicKeyCose(reg.getCredential().getPublicKeyCose())
		                    .signatureCount(reg.getSignatureCount())
		                    .build()
		                )
		                .collect(Collectors.toSet()));
		    }
	 
}
