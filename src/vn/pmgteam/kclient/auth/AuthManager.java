package vn.pmgteam.kclient.auth;

import vn.pmgteam.kclient.MinecraftLauncher;

public class AuthManager {

    private static Account currentAccount;

    public static MinecraftLauncher.Session loginOffline(String username) {
        currentAccount = new Account(username, "abcdef123456789", "token-fake-" + username, true);
        return buildSession();
    }

    public static MinecraftLauncher.Session loginLocal(String username, String password) {
        currentAccount = new Account(username, "abcdef123456789", "token-fake-" + username, true);
        return buildSession();
    }

    public static MinecraftLauncher.Session loginTenAuth(String username, String password) {
        currentAccount = new Account(username, "abcdef123456789", "token-fake-" + username, true);
        return buildSession();
    }

    public static MinecraftLauncher.Session loginMicrosoft() {
        currentAccount = new Account("Player", "abcdef123456789", "token-fake-ms", true);
        return buildSession();
    }

    private static MinecraftLauncher.Session buildSession() {
        return new MinecraftLauncher.Session(
                currentAccount.getUsername(),
                currentAccount.getUuid(),
                currentAccount.getToken()
        );
    }

    public static boolean hasFullLicense() {
        return currentAccount != null && currentAccount.isFullLicense();
    }

    public static Account getCurrentAccount() { return currentAccount; }
}
