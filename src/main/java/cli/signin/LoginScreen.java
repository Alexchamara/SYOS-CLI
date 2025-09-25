package cli.signin;

import application.usecase.AuthenticationUseCase;
import domain.user.Role;

import java.util.Scanner;

public final class LoginScreen {
    private final AuthenticationUseCase auth;

    public LoginScreen(AuthenticationUseCase auth){ this.auth = auth; }

    public AuthenticationUseCase.Session prompt() {
        var sc = new Scanner(System.in);
        System.out.println("=== Login ===");
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        try {
            var session = auth.loginStaff(u, p);

            if (session.role() == Role.USER) {
                throw new IllegalArgumentException("USER role is not allowed to access CLI interface. Please use the Web Shop interface.");
            }

            System.out.println("Welcome, " + session.identifier() + " (" + session.role() + ")");
            return session;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void route(AuthenticationUseCase.Session session, Runnable cashierMenu, Runnable managerMenu) {
        if (session == null) return;
        if (session.role() == Role.MANAGER) managerMenu.run(); else cashierMenu.run();
    }
}