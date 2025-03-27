import java.util.Scanner;

public class Menu {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n1. Afisare ");
            System.out.println("2. Adauga o carte");
            System.out.println("3. Sterge o carte");
            System.out.println("4. Adauga o copie");
            System.out.println("5. Iesire");
            System.out.print("Alege o optiune: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    GUI viewer = new GUI();
                    viewer.setVisible(true);
                    break;
                case 2:
                    InsertBook.insertBook(scanner);
                    break;
                case 3:
                    DeleteBook.deleteBook(scanner);
                    break;
                case 4:
                    AddBookPublisher.addBookPublisher(scanner);
                    break;
                case 5:
                    System.out.println("La revedere!");
                    return;
                default:
                    System.out.println("Optiune invalida! Incearca din nou.");
                    break;
            }
        }
    }
}
