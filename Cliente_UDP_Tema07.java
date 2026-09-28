import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Cliente_UDP_Tema07 {

    private static final int CHUNK_SIZE = 1024; // bytes de imagem por pacote (evita fragmentação no nível IP)
    private static final int PORTA_SERVIDOR = 9877;

    public static void main(String[] args) throws Exception {
        String ipServidor = (args.length > 0) ? args[0] : "";
        InetAddress enderecoServidor = InetAddress.getByName(ipServidor);

        DatagramSocket clientSocket = new DatagramSocket();
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Cliente UDP (Tema 07) - servidor: " + ipServidor + ":" + PORTA_SERVIDOR);
        System.out.println("Copie uma imagem (Print Screen) e digite 'print' para enviar, ou 'sair' para encerrar.");

        while (true) {
            System.out.print("> ");
            String comando = console.readLine();

            if (comando == null || comando.equalsIgnoreCase("sair")) {
                break;
            }

            if (comando.equalsIgnoreCase("print")) {
                byte[] imagemBytes = getClipboardImageBytes();

                if (imagemBytes == null) {
                    System.out.println("Nenhuma imagem encontrada na área de transferência.");
                    continue;
                }

                enviarImagemUDP(clientSocket, enderecoServidor, imagemBytes);
            }
        }

        clientSocket.close();
    }

    private static void enviarImagemUDP(DatagramSocket socket, InetAddress endereco, byte[] imagem) throws IOException {
        int totalChunks = (int) Math.ceil((double) imagem.length / CHUNK_SIZE);

        // 1) Pacote INIT: avisa ao servidor quantos pacotes virão e o tamanho total
        ByteArrayOutputStream initBuffer = new ByteArrayOutputStream();
        DataOutputStream initStream = new DataOutputStream(initBuffer);
        initStream.writeByte(0); // tipo 0 = INIT
        initStream.writeInt(totalChunks);
        initStream.writeInt(imagem.length);
        enviarPacote(socket, endereco, initBuffer.toByteArray());

        System.out.println("Enviando imagem (" + imagem.length + " bytes) em " + totalChunks + " pacote(s)...");

        // 2) Pacotes DATA: cada um com número de sequência + um pedaço da imagem
        for (int i = 0; i < totalChunks; i++) {
            int inicio = i * CHUNK_SIZE;
            int fim = Math.min(inicio + CHUNK_SIZE, imagem.length);

            ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(dataBuffer);
            dataStream.writeByte(1); // tipo 1 = DATA
            dataStream.writeInt(i);  // número de sequência (essencial: UDP não garante ordem)
            dataStream.write(imagem, inicio, fim - inicio);

            enviarPacote(socket, endereco, dataBuffer.toByteArray());
        }

        // 3) Pacote END: avisa que o envio terminou
        ByteArrayOutputStream endBuffer = new ByteArrayOutputStream();
        DataOutputStream endStream = new DataOutputStream(endBuffer);
        endStream.writeByte(2); // tipo 2 = END
        endStream.writeInt(totalChunks);
        enviarPacote(socket, endereco, endBuffer.toByteArray());

        System.out.println("Envio concluído!");
    }

    private static void enviarPacote(DatagramSocket socket, InetAddress endereco, byte[] dados) throws IOException {
        DatagramPacket pacote = new DatagramPacket(dados, dados.length, endereco, PORTA_SERVIDOR);
        socket.send(pacote);
    }

    /**
     * Lê a imagem atual da área de transferência do sistema (se houver)
     * e a converte para um array de bytes no formato PNG.
     */
    private static byte[] getClipboardImageBytes() throws Exception {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        if (!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            return null;
        }

        Image imagem = (Image) clipboard.getData(DataFlavor.imageFlavor);

        BufferedImage imagemBuffer = new BufferedImage(
                imagem.getWidth(null), imagem.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = imagemBuffer.createGraphics();
        g2d.drawImage(imagem, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imagemBuffer, "png", baos);
        return baos.toByteArray();
    }
}
