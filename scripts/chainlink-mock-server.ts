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

        // console.log('Staring server!');
        this.isRunning = true;
        this.main();
    }

    public stop() {
        // console.log('Stoping server!');
        this.isRunning = false;
    }

    private async main() {
        if (this.isRunning === false) return;

        if (this.contract !== null) {            
            const hasCodeToExecute = await this.contract.hasCodeToExecute();

            // console.log('Has Code to execute: ', hasCodeToExecute);

            if (hasCodeToExecute) {
                // console.log('\tExecuting code.');

                const code = await this.contract.getCodeToExecute();
                const args = await this.contract.getCodeToExecuteArgs();

                const output = await executeCode(code, args);

                const response = await this.contract.setCodeToExecuteResponse(output);

                await response.wait();
            }
        }

        setTimeout(() => this.main(), 3000);
    }

    public setContract(contract: ChainlinkFunctionsMock) {
        // console.log('Setting contract!');
        this.contract = contract;
    }
}

export default new ChainlinkServer();