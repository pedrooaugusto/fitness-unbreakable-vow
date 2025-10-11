import { ChainlinkFunctionsMock } from '../typechain-types';
import { executeCode } from './execute-code-oracle';

class ChainlinkServer {
    private contract: ChainlinkFunctionsMock | null = null;
    private isRunning = false;

    public start() {
        if (this.isRunning) {
            this.isRunning = false;
            throw new Error('Already running.');
        }

        console.log('[ChainlinkServer] Staring server!');
        this.isRunning = true;
        this.main();
    }

    public stop() {
        console.log('[ChainlinkServer] Stoping server!');
        this.isRunning = false;
    }

    private async main() {
        if (this.isRunning === false) return;

        if (this.contract !== null) {            
            const hasCodeToExecute = await this.contract.hasCodeToExecute();

            // console.log('[CHAINLINK] Checking for code to execute: ' + hasCodeToExecute);

            if (hasCodeToExecute) {
                try {
                    console.log('[ChainlinkServer] Executing code.');

                    const code = await this.contract.getCodeToExecute();
                    const args = await this.contract.getCodeToExecuteArgs();

                    const output = await executeCode(code, args);
                    
                    console.log('[ChainlinkServer] Done executing code. Sending response.');

                    const response = await this.contract.setCodeToExecuteResponse(output);

                    const receipt = await response.wait();

                    console.log('[ChainlinkServer] Response sent.');

                    const parsedLogs = (receipt?.logs || []).map((log) => this.contract!.interface.parseLog(log));

                    console.log('[ChainlinkServer] Execution Output: ', parsedLogs[0]?.args);

                    // return receipt;
                } catch(e) {
                    console.error(e);
                }
            }
        }

        setTimeout(() => this.main(), 100);
    }

    public setContract(contract: ChainlinkFunctionsMock) {
        console.log('[ChainlinkServer] Setting contract!');
        this.contract = contract;
    }
}

export default new ChainlinkServer();