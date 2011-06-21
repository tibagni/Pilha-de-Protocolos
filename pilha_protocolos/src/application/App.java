/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application;

import java.util.Scanner;
import pilha_protocolos.Utilities;

/**
 * 
 * @author Tiago
 */
public class App {
    private FileTransfer fileTransferInstance;
    private Scanner input = new Scanner(System.in);
    private final Object locker = new Object();

    public void startApp() {
        print("Sistema de transferencia de arquivos:");
        printLine();
        while (true) {
            try {
                synchronized (locker) {
                    executeCommand(input.nextLine());

                }
            } catch (Exception e) {
                printErr("Nao foi possivel executar comando\n"
                        + "'help' para a lista completa");
            }
        }
    }

    /*package*/ void requestConfirmation(FileInfo info) {
        synchronized (locker) {
            String fInfo = "Nome do arquivo: " + info.getName() + "\n"
                    + "Tamanho: " + info.getSize();
            print(fInfo);
            print("accept/deny ?");
            executeCommand(input.nextLine());
        }
    }
    private void print(String s, Object... args) {
        System.out.printf(s, args);
        System.out.println();
    }
    private void printErr(String s, Object... args) {
        System.err.printf(s, args);
        System.out.println();
    }
    private void printLine() {
        System.out.println("---------------------"
                + "---------------------------");
    }
    private synchronized boolean executeCommand(String command) {
        boolean commandExecuted = false;
        String[] commandParts = command.split(" ");

        if (command == null || command.length() == 0) return false;
        // Server Commands - Quem recebe os arquivos
        if ("waitfor".equals(commandParts[0])) {
            if (fileTransferInstance == null) {
                // espera conexoes para receber arquivos
                try {
                    int port = Integer.parseInt(commandParts[1]);
                    // Cria a instancia server de FileTransfer
                    fileTransferInstance = new FileTransfer(port);
                    // Espera requisicao de envio de arquivo
                    if (fileTransferInstance.receive()) {
                        FileInfo fI = fileTransferInstance.getFileInfo();
                        if (fI != null) {
                            requestConfirmation(fI);
                        }
                    }
                } catch (Exception ex) {
                    Utilities.logException(ex);
                    printErr("Erro no comando, sintaxe correta: waitfor ip:porta");
                }
                commandExecuted = true;
            }
        } else if ("accept".equals(commandParts[0])) {
            // aceita requisicoes de transferencia de arquivo
            if (fileTransferInstance == null ||
                    fileTransferInstance.whichStep() != FileTransfer.STEP_PENDING_REQUEST) {
                printErr("Use 'accept' para aceitar requisicoes de transferencia de arquivo"
                        + "\nNao existem requisicoes pendentes!");
            } else {
                if (fileTransferInstance.sendResponse("accept")) {
                    commandExecuted = true;
                    if (fileTransferInstance.receive()) {
                        print("Arquivo recebido com sucesso!");
                    }
                    // Espera requisicao de envio de arquivo
                    if (fileTransferInstance.receive()) {
                        FileInfo fI = fileTransferInstance.getFileInfo();
                        if (fI != null) {
                            requestConfirmation(fI);
                        }
                    }
                }
            }
        } else if ("deny".equals(commandParts[0])) {
            // recusa requisicao de transferencia de arquivo
            if (fileTransferInstance == null ||
                    fileTransferInstance.whichStep() != FileTransfer.STEP_PENDING_REQUEST) {
                printErr("Use 'deny' para recusar requisicoes de transferencia de arquivo"
                        + "\nNao existem requisicoes pendentes!");
            } else {
                if (fileTransferInstance.sendResponse("deny")) {
                    commandExecuted = true;
                }
            }
        }
        // Client commands - Quem envia os arquivos
        else if ("connect".equals(commandParts[0])) {
            if (fileTransferInstance == null) {
                String ipPort[] = commandParts[1].split(":");
                if (ipPort == null || ipPort.length != 2) {
                    printErr("Erro no comando, sintaxe correta: connect ip:porta");
                } else {
                    // Tenta converter ip e porta em ints e depois conectar com o server
                    try {
                        int port = Integer.parseInt(ipPort[1]);
                        fileTransferInstance = new FileTransfer(ipPort[0], port, 123);
                        commandExecuted = true;
                    } catch (Exception e) {
                        Utilities.logException(e);
                        printErr("Ip ou porta incorretos!!");
                    }
                }
            }
        } else if ("send".equals(commandParts[0])) {
            String filename = commandParts[1];
            if (fileTransferInstance == null ||
                    fileTransferInstance.whichStep() != FileTransfer.STEP_WAITING_TO_SEND) {
                printErr("Nao e possivel enviar um arquivo neste momento!");
            } else {
                if (filename != null && filename.length() > 0) {
                    // envia request para enviar arquivo.
                    // em caso positivo o arquivo sera enviado automaticamente
                    // pela classe FileTransfer
                    fileTransferInstance.sendRequestToSend(filename);
                    // espera confirmacao
                    if (fileTransferInstance.receive()) {
                        // Envia o arquivo - confirmacao positiva
                        print("enviando!! file");
                        fileTransferInstance.sendFile(filename);
                    }
                }
            }
        } else if ("close".equals(commandParts[0])) {
            // TODO fecha a conexao, encerra o programa
        } else if("help".equals(commandParts[0])) {
            print("Servidor - Recebe arquivos");
            printLine();
            print("waitfor portToListen - escuta requisicoes de envio "
                    + "de arquivos em uma porta especifica\n");
            print("accept - aceita transferencia\n");
            print("deny - recusa transferencia");
            print(""); //println()
            printLine();
            print("Cliente - Envia arquivos");
            printLine();
            print("connect serverId:port - Conecta em um servidor na porta especificada\n");
            print("send filename - envia arquivo 'filename'\n");
            print("close - encerra execucao do programa\n");
            printLine();
            print("help - exibe este texto");
        } else {
            printErr("%s nao e um comando conhecido"
                    + "\n'help' para maiores informacoes", commandParts[0]);
        }
        return commandExecuted;
    }
}
