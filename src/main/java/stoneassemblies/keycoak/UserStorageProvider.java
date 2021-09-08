package stoneassemblies.keycoak;

import stoneassemblies.keycoak.interfaces.UserRepository;
import stoneassemblies.keycoak.models.User;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class UserStorageProvider implements org.keycloak.storage.UserStorageProvider,
        UserLookupProvider, UserQueryProvider, CredentialInputUpdater, CredentialInputValidator {

    private Logger logger = Logger.getLogger(UserStorageProvider.class.getName());

    private final KeycloakSession session;

    private final ComponentModel model;

    private final UserRepository repository;

    public UserStorageProvider(KeycloakSession session, ComponentModel model, UserRepository repository) {
        logger.info("Created UserStorageProvider");

        this.session = session;
        this.model = model;
        this.repository = repository;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel) input;
        return repository.validateCredentials(user.getUsername(), cred.getChallengeResponse());
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel) input;
        return repository.updateCredentials(user.getUsername(), cred.getChallengeResponse());
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.emptySet();
    }

    @Override
    public void preRemove(RealmModel realm) {
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        logger.info("getUserById => " + id);

        String externalId = StorageId.externalId(id);
        User userById = repository.findUserById(externalId);
        if (userById != null) {
            return new UserAdapterFederatedStorage(session, realm, model, userById);
        }

        return null;
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        logger.info("getUserByUsername => " + username);

        User userByUsernameOrEmail = repository.findUserByUsernameOrEmail(username);
        if (userByUsernameOrEmail != null) {
            return new UserAdapterFederatedStorage(session, realm, model, userByUsernameOrEmail);
        }

        return null;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        logger.info("getUserByEmail => " + email);

        return getUserByUsername(email, realm);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        logger.info("getUsersCount");

        return repository.getUsersCount();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realmModel) {
        logger.info("getUsers");
        int usersCount = this.getUsersCount(realmModel);
        return new List<UserModel>() {
            @Override
            public int size() {
                return usersCount;
            }

            @Override
            public boolean isEmpty() {
                return usersCount > 0;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<UserModel> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(UserModel userModel) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends UserModel> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends UserModel> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public UserModel get(int index) {
                return null;
            }

            @Override
            public UserModel set(int index, UserModel element) {
                return null;
            }

            @Override
            public void add(int index, UserModel element) {

            }

            @Override
            public UserModel remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<UserModel> listIterator() {
                return null;
            }

            @Override
            public ListIterator<UserModel> listIterator(int index) {
                return null;
            }

            @Override
            public List<UserModel> subList(int fromIndex, int toIndex) {
                return null;
            }
        };
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        logger.info("getUsers => " + firstResult + "," + maxResults);

        return repository.getUsers(firstResult, maxResults).stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        logger.info("searchForUser => " + search);

        return repository.findUsers(search).stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        logger.info("searchForUser => " + search + "," + firstResult + ", " + maxResults);

        return repository.findUsers(search, firstResult, maxResults).stream()
                .map(user -> new UserAdapterFederatedStorage(session, realm, model, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        logger.info("searchForUser Map" );

        return this.getUsers(realm);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        logger.info("searchForUser Map => " + firstResult + ", " + maxResults);

        return getUsers(realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.emptyList();
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        logger.info("searchForUserByUserAttribute =>" + attrValue);

        return Collections.emptyList();
    }
}