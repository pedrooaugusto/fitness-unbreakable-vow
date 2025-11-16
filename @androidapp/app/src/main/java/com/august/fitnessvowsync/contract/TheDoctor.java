package com.august.fitnessvowsync.contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.7.0.
 */
@SuppressWarnings("rawtypes")
public class TheDoctor extends Contract {
    public static class CustomError { public CustomError(Object a, Object b){} }
    public static final String BINARY = "610120604052346100ac5761001b61001561017f565b91610308565b6100236100b1565b61098b6103d082396080518181816102720152610726015260a0518181816102cb0152818161068901526106ff015260c051818181610199015281816106c3015281816107ca01528181610832015281816108bc01526108fe015260e051818181610359015281816107eb015261091f0152610100518181816104470152610759015261098b90f35b6100b7565b60405190565b600080fd5b601f801991011690565b634e487b7160e01b600052604160045260246000fd5b906100e6906100bc565b810190811060018060401b038211176100fe57604052565b6100c6565b9061011661010f6100b1565b92836100dc565b565b600080fd5b90565b6101298161011d565b0361013057565b600080fd5b9050519061014282610120565b565b909160608284031261017a576101776101608460008501610135565b9361016e8160208601610135565b93604001610135565b90565b610118565b61019d610d5b8038038061019281610103565b928339810190610144565b909192565b90565b90565b6101bc6101b76101c1926101a2565b6101a5565b61011d565b90565b634e487b7160e01b600052601260045260246000fd5b634e487b7160e01b600052601160045260246000fd5b6101fc6102029161011d565b9161011d565b90811561020d570490565b6101c4565b90565b61022961022461022e92610212565b6101a5565b61011d565b90565b6102406102469193929361011d565b9261011d565b820391821161025157565b6101da565b610260905161011d565b90565b60ff1690565b61027d6102786102829261011d565b6101a5565b610263565b90565b61028f9051610263565b90565b6102a66102a16102ab92610263565b6101a5565b61011d565b90565b6102bd6102c39193929361011d565b9261011d565b916102cf83820261011d565b9281840414901517156102de57565b6101da565b6102f26102f89193929361011d565b9261011d565b820180921161030357565b6101da565b9061035c61034c61036192610342610331610395978060805261032b60056101a8565b906101f0565b61033c610e10610215565b9061039f565b60e0528490610231565b6103566080610256565b906101f0565b610269565b610100528060a05261038f610377610100610285565b61038a6103846080610256565b91610292565b6102ae565b906102e3565b60c052565b600090565b6103a761039a565b50806103bb6103b58461011d565b9161011d565b116000146103c857505b90565b90506103c556fe60806040526004361015610013575b610508565b61001e6000356100cd565b806301a1a44e146100c85780633e6eb0c4146100c3578063480d01bb146100be5780634e7c3065146100b95780638485eb35146100b45780638f5949f9146100af578063c1a287e2146100aa578063c29ae2a6146100a5578063d1e9ff11146100a0578063d80f98471461009b5763ebfb7af60361000e576104d3565b61049e565b610469565b610410565b61037b565b610322565b6102ed565b610294565b61023b565b6101de565b610152565b60e01c90565b60405190565b600080fd5b600080fd5b90565b6100ef816100e3565b036100f657565b600080fd5b90503590610108826100e6565b565b9060208282031261012457610121916000016100fb565b90565b6100de565b60ff1690565b61013890610129565b9052565b91906101509060006020850194019061012f565b565b346101825761017e61016d61016836600461010a565b610674565b6101756100d3565b9182918261013c565b0390f35b6100d9565b600091031261019257565b6100de565b7f000000000000000000000000000000000000000000000000000000000000000090565b6101c4906100e3565b9052565b91906101dc906000602085019401906101bb565b565b3461020e576101ee366004610187565b61020a6101f9610197565b6102016100d3565b918291826101c8565b0390f35b6100d9565b151590565b61022190610213565b9052565b919061023990600060208501940190610218565b565b3461026b5761024b366004610187565b6102676102566107b5565b61025e6100d3565b91829182610225565b0390f35b6100d9565b7f000000000000000000000000000000000000000000000000000000000000000090565b346102c4576102a4366004610187565b6102c06102af610270565b6102b76100d3565b918291826101c8565b0390f35b6100d9565b7f000000000000000000000000000000000000000000000000000000000000000090565b3461031d576102fd366004610187565b6103196103086102c9565b6103106100d3565b918291826101c8565b0390f35b6100d9565b3461035257610332366004610187565b61034e61033d610820565b6103456100d3565b91829182610225565b0390f35b6100d9565b7f000000000000000000000000000000000000000000000000000000000000000090565b346103ab5761038b366004610187565b6103a7610396610357565b61039e6100d3565b918291826101c8565b0390f35b6100d9565b634e487b7160e01b600052602160045260246000fd5b600311156103d057565b6103b0565b906103df826103c6565b565b6103ea906103d5565b90565b6103f6906103e1565b9052565b919061040e906000602085019401906103ed565b565b3461044057610420366004610187565b61043c61042b610865565b6104336100d3565b918291826103fa565b0390f35b6100d9565b7f000000000000000000000000000000000000000000000000000000000000000090565b3461049957610479366004610187565b610495610484610445565b61048c6100d3565b9182918261013c565b0390f35b6100d9565b346104ce576104ae366004610187565b6104ca6104b9610895565b6104c16100d3565b9182918261013c565b0390f35b6100d9565b34610503576104e3366004610187565b6104ff6104ee6108aa565b6104f66100d3565b91829182610225565b0390f35b6100d9565b600080fd5b600090565b60209181520190565b60007f212120576962626c7920576f62626c792054696d65792057696d657920212100910152565b610550601f602092610512565b6105598161051b565b0190565b6105739060208101906000818303910152610543565b90565b1561057d57565b6105856100d3565b62461bcd60e51b81528061059b6004820161055d565b0390fd5b90565b90565b6105b96105b46105be9261059f565b6105a2565b610129565b90565b634e487b7160e01b600052601160045260246000fd5b6105e36105e991610129565b91610129565b90039060ff82116105f657565b6105c1565b61060a610610919392936100e3565b926100e3565b820391821161061b57565b6105c1565b634e487b7160e01b600052601260045260246000fd5b610642610648916100e3565b916100e3565b908115610653570490565b610620565b61066c610667610671926100e3565b6105a2565b610129565b90565b61067c61050d565b506106ba816106b36106ad7f00000000000000000000000000000000000000000000000000000000000000006100e3565b916100e3565b1015610576565b806106ed6106e77f00000000000000000000000000000000000000000000000000000000000000006100e3565b916100e3565b10156107535761074b610724610750927f0000000000000000000000000000000000000000000000000000000000000000906105fb565b7f000000000000000000000000000000000000000000000000000000000000000090610636565b610658565b90565b506107887f000000000000000000000000000000000000000000000000000000000000000061078260016105a5565b906105d7565b90565b600090565b61079f6107a5919392936100e3565b926100e3565b82018092116107b057565b6105c1565b6107bd61078b565b504261081b6108156108107f00000000000000000000000000000000000000000000000000000000000000007f000000000000000000000000000000000000000000000000000000000000000090610790565b6100e3565b916100e3565b101590565b61082861078b565b504261085c6108567f00000000000000000000000000000000000000000000000000000000000000006100e3565b916100e3565b1090565b600090565b61086d610860565b506108766107b5565b61089057610882610820565b61088b57600190565b600090565b600290565b61089d61050d565b506108a742610674565b90565b6108b261078b565b50426108e66108e07f00000000000000000000000000000000000000000000000000000000000000006100e3565b916100e3565b1015806108f1575b90565b504261094f6109496109447f00000000000000000000000000000000000000000000000000000000000000007f000000000000000000000000000000000000000000000000000000000000000090610790565b6100e3565b916100e3565b106108ee56fea26469706673582212203fd1aa4295118e9fd5e961bb6434d20d5a421ac26e5691dec90f69feff27c07964736f6c634300081c0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_CREATION_DATE = "CREATION_DATE";

    public static final String FUNC_EXPIRATION_DATE = "EXPIRATION_DATE";

    public static final String FUNC_GRACE_PERIOD = "GRACE_PERIOD";

    public static final String FUNC_NUMBER_OF_WEEKS = "NUMBER_OF_WEEKS";

    public static final String FUNC_SECONDS_IN_ONE_WEEK = "SECONDS_IN_ONE_WEEK";

    public static final String FUNC_GETCONTRACTPHASE = "getContractPhase";

    public static final String FUNC_GETCURRENTWEEKINDEX = "getCurrentWeekIndex";

    public static final String FUNC_GETWEEKINDEXOF = "getWeekIndexOf";

    public static final String FUNC_ISCONTRACTACTIVE = "isContractActive";

    public static final String FUNC_ISCONTRACTFULLYEXPIRED = "isContractFullyExpired";

    public static final String FUNC_ISCONTRACTINGRACEPERIOD = "isContractInGracePeriod";

    @Deprecated
    protected TheDoctor(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TheDoctor(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TheDoctor(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TheDoctor(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<BigInteger> CREATION_DATE() {
        final Function function = new Function(FUNC_CREATION_DATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> EXPIRATION_DATE() {
        final Function function = new Function(FUNC_EXPIRATION_DATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> GRACE_PERIOD() {
        final Function function = new Function(FUNC_GRACE_PERIOD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> NUMBER_OF_WEEKS() {
        final Function function = new Function(FUNC_NUMBER_OF_WEEKS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> SECONDS_IN_ONE_WEEK() {
        final Function function = new Function(FUNC_SECONDS_IN_ONE_WEEK, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getContractPhase() {
        final Function function = new Function(FUNC_GETCONTRACTPHASE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getCurrentWeekIndex() {
        final Function function = new Function(FUNC_GETCURRENTWEEKINDEX, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getWeekIndexOf(BigInteger timestamp) {
        final Function function = new Function(FUNC_GETWEEKINDEXOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(timestamp)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> isContractActive() {
        final Function function = new Function(FUNC_ISCONTRACTACTIVE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isContractFullyExpired() {
        final Function function = new Function(FUNC_ISCONTRACTFULLYEXPIRED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isContractInGracePeriod() {
        final Function function = new Function(FUNC_ISCONTRACTINGRACEPERIOD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static TheDoctor load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new TheDoctor(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TheDoctor load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TheDoctor(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TheDoctor load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new TheDoctor(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TheDoctor load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TheDoctor(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TheDoctor> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek)));
        return deployRemoteCall(TheDoctor.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<TheDoctor> deploy(Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek)));
        return deployRemoteCall(TheDoctor.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TheDoctor> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek)));
        return deployRemoteCall(TheDoctor.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TheDoctor> deploy(Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek)));
        return deployRemoteCall(TheDoctor.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }
}
