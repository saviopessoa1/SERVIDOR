import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Servidor_UDP_Tema07 {

    private static final int PORTA = 9877;
    private static final String PASTA_DESTINO = "imagens_recebidas";

    // Guarda o progresso de recepção de cada cliente (chave = IP:porta),
    // permitindo receber de mais de um cliente ao mesmo tempo sem misturar os dados.
    private static Map<String, RecepcaoImagem> recepcoesEmAndamento = new HashMap<>();

    public static void main(String[] args) {
        try {
            File pasta = new File(PASTA_DESTINO);
            if (!pasta.exists()) {
                pasta.mkdirs();
            }

            DatagramSocket serverSocket = new DatagramSocket(PORTA);
            byte[] bufferRecepcao = new byte[2048]; // maior que CHUNK_SIZE do cliente + cabeçalho do protocolo

            System.out.println("Servidor UDP (Tema 07) aguardando imagens na porta " + PORTA + "...");
            System.out.println("Pasta de destino: " + pasta.getAbsolutePath());

            while (true) {
                DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecepcao, bufferRecepcao.length);
                serverSocket.receive(pacoteRecebido);
                processarPacote(pacoteRecebido);
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }

    private static void processarPacote(DatagramPacket pacote) throws IOException {
        String chaveCliente = pacote.getAddress().getHostAddress() + ":" + pacote.getPort();

        DataInputStream entrada = new DataInputStream(
                new ByteArrayInputStream(pacote.getData(), 0, pacote.getLength()));
        int tipo = entrada.readByte();

        if (tipo == 0) { // INIT
            int totalChunks = entrada.readInt();
            int tamanhoTotal = entrada.readInt();

            recepcoesEmAndamento.put(chaveCliente, new RecepcaoImagem(totalChunks));
            System.out.println("[" + chaveCliente + "] iniciando recepcao: "
                    + totalChunks + " pacotes, " + tamanhoTotal + " bytes no total.");

        } else if (tipo == 1) { // DATA
            int sequencia = entrada.readInt();
            int tamanhoPedaco = pacote.getLength() - 5; // 1 byte (tipo) + 4 bytes (sequência)
            byte[] pedaco = new byte[tamanhoPedaco];
            entrada.readFully(pedaco);

            RecepcaoImagem recepcao = recepcoesEmAndamento.get(chaveCliente);
            if (recepcao != null) {
                recepcao.adicionarPedaco(sequencia, pedaco);
            } else {
                System.out.println("[" + chaveCliente + "] pacote DATA recebido sem INIT anterior - descartado.");
            }

        } else if (tipo == 2) { // END
            RecepcaoImagem recepcao = recepcoesEmAndamento.remove(chaveCliente);
            if (recepcao != null) {
                byte[] imagemCompleta = recepcao.montarImagemCompleta();
                salvarImagem(imagemCompleta);
            }
        }
    }

    private static void salvarImagem(byte[] dadosImagem) throws IOException {
        String nomeArquivo = PASTA_DESTINO + File.separator + "print_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date()) + ".png";

        try (FileOutputStream fos = new FileOutputStream(nomeArquivo)) {
            fos.write(dadosImagem);
        }

        System.out.println("Imagem salva em: " + nomeArquivo + " (" + dadosImagem.length + " bytes)");
    }

    /**
     * Acumula os pedaços (chunks) de uma imagem até que todos cheguem,
     * remontando-a na ordem correta pelo número de sequência.
     */
    private static class RecepcaoImagem {
        private final byte[][] pedacos;

        RecepcaoImagem(int totalChunks) {
            this.pedacos = new byte[totalChunks][];
        }

        void adicionarPedaco(int sequencia, byte[] dado) {
            if (sequencia >= 0 && sequencia < pedacos.length) {
                pedacos[sequencia] = dado;
            }
        }

        byte[] montarImagemCompleta() throws IOException {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            int perdidos = 0;

            for (byte[] pedaco : pedacos) {
                if (pedaco != null) {
                    saida.write(pedaco);
                } else {
                    perdidos++;
                }
            }

            if (perdidos > 0) {
                System.out.println("AVISO: " + perdidos + " pacote(s) nao chegaram - a imagem "
                        + "pode estar corrompida (limitacao esperada do UDP, que nao tem confirmacao/retransmissao).");
            }

            return saida.toByteArray();
        }
    }
}
