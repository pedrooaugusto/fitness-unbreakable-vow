package com.august.fitnessvowsync.contract;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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
    public static final String BINARY = "61014060405234610099576100216100156101bd565b9493909392919261041a565b61002961009e565b61114d61050d82396080518181816103bf0152610a2a015260a0518181816103230152818161092101528181610ba50152610bf3015260c05181818161048701528181610c5c0152610d4b015260e05181610e5401526101005181610e7d01526101205181610ea7015261114d90f35b6100a4565b60405190565b600080fd5b601f801991011690565b634e487b7160e01b600052604160045260246000fd5b906100d3906100a9565b810190811060018060401b038211176100eb57604052565b6100b3565b906101036100fc61009e565b92836100c9565b565b600080fd5b90565b6101168161010a565b0361011d57565b600080fd5b9050519061012f8261010d565b565b90565b61013d81610131565b0361014457565b600080fd5b9050519061015682610134565b565b909160c0828403126101b8576101718360008401610122565b9261017f8160208501610122565b9261018d8260408301610122565b926101b561019e8460608501610149565b936101ac8160808601610149565b9360a001610149565b90565b610105565b6101db61165a803803806101d0816100f0565b928339810190610158565b909192939495565b90565b90565b6101fd6101f8610202926101e3565b6101e6565b61010a565b90565b634e487b7160e01b600052601260045260246000fd5b634e487b7160e01b600052601160045260246000fd5b61023d6102439161010a565b9161010a565b90811561024e570490565b610205565b90565b61026a61026561026f92610253565b6101e6565b61010a565b90565b6102816102879193929361010a565b9261010a565b820391821161029257565b61021b565b6102a1905161010a565b90565b60ff1690565b6102be6102b96102c39261010a565b6101e6565b6102a4565b90565b60001b90565b906102d860ff916102c6565b9181191691161790565b6102f66102f16102fb926102a4565b6101e6565b6102a4565b90565b90565b9061031661031161031d926102e2565b6102fe565b82546102cc565b9055565b9061032e600019916102c6565b9181191691161790565b61034c6103476103519261010a565b6101e6565b61010a565b90565b90565b9061036c61036761037392610338565b610354565b8254610321565b9055565b60001c90565b60ff1690565b61038f61039491610377565b61037d565b90565b6103a19054610383565b90565b6103b86103b36103bd926102a4565b6101e6565b61010a565b90565b6103cf6103d59193929361010a565b9261010a565b916103e183820261010a565b9281840414901517156103f057565b61021b565b61040461040a9193929361010a565b9261010a565b820180921161041557565b61021b565b61048761048061047b61046b6104bd956104616104506104c49961043c6104d1565b8060a05261044a60056101e9565b90610231565b61045b610e10610256565b906104dc565b60c0528590610272565b61047560a0610297565b90610231565b6102aa565b6002610301565b610492816000610357565b6104b761049f6002610397565b6104b26104ac60a0610297565b916103a4565b6103c0565b906103f5565b6001610357565b60e0526101005261012052565b33608052565b600090565b6104e46104d7565b50806104f86104f28461010a565b9161010a565b1160001461050557505b90565b905061050256fe60806040526004361015610013575b610709565b61001e6000356100fd565b806301a1a44e146100f85780631dc43637146100f35780633e6eb0c4146100ee578063480d01bb146100e95780634e7c3065146100e45780638485eb35146100df5780638da5cb5b146100da5780638f5949f9146100d5578063c1a287e2146100d0578063c29ae2a6146100cb578063d1e9ff11146100c6578063d80f9847146100c1578063ebfb7af6146100bc5763ee1ebc7f0361000e576106d4565b610619565b6105e4565b6105af565b61053e565b6104a9565b610450565b61041b565b610388565b610345565b6102ec565b61028f565b6101eb565b610182565b60e01c90565b60405190565b600080fd5b600080fd5b90565b61011f81610113565b0361012657565b600080fd5b9050359061013882610116565b565b90602082820312610154576101519160000161012b565b90565b61010e565b60ff1690565b61016890610159565b9052565b91906101809060006020850194019061015f565b565b346101b2576101ae61019d61019836600461013a565b6108b4565b6101a5610103565b9182918261016c565b0390f35b610109565b91906040838203126101e057806101d46101dd926000860161012b565b9360200161012b565b90565b61010e565b60000190565b3461021a576102046101fe3660046101b7565b90610c2c565b61020c610103565b80610216816101e5565b0390f35b610109565b600091031261022a57565b61010e565b1c90565b90565b61024690600861024b930261022f565b610233565b90565b906102599154610236565b90565b610269600160009061024e565b90565b61027590610113565b9052565b919061028d9060006020850194019061026c565b565b346102bf5761029f36600461021f565b6102bb6102aa61025c565b6102b2610103565b91829182610279565b0390f35b610109565b151590565b6102d2906102c4565b9052565b91906102ea906000602085019401906102c9565b565b3461031c576102fc36600461021f565b610318610307610c3d565b61030f610103565b918291826102d6565b0390f35b610109565b7f000000000000000000000000000000000000000000000000000000000000000090565b346103755761035536600461021f565b610371610360610321565b610368610103565b91829182610279565b0390f35b610109565b61038560008061024e565b90565b346103b85761039836600461021f565b6103b46103a361037a565b6103ab610103565b91829182610279565b0390f35b610109565b7f000000000000000000000000000000000000000000000000000000000000000090565b60018060a01b031690565b6103f5906103e1565b90565b610401906103ec565b9052565b9190610419906000602085019401906103f8565b565b3461044b5761042b36600461021f565b6104476104366103bd565b61043e610103565b91829182610405565b0390f35b610109565b346104805761046036600461021f565b61047c61046b610c91565b610473610103565b918291826102d6565b0390f35b610109565b7f000000000000000000000000000000000000000000000000000000000000000090565b346104d9576104b936600461021f565b6104d56104c4610485565b6104cc610103565b91829182610279565b0390f35b610109565b634e487b7160e01b600052602160045260246000fd5b600311156104fe57565b6104de565b9061050d826104f4565b565b61051890610503565b90565b6105249061050f565b9052565b919061053c9060006020850194019061051b565b565b3461056e5761054e36600461021f565b61056a610559610cbf565b610561610103565b91829182610528565b0390f35b610109565b60ff1690565b61058990600861058e930261022f565b610573565b90565b9061059c9154610579565b90565b6105ac6002600090610591565b90565b346105df576105bf36600461021f565b6105db6105ca61059f565b6105d2610103565b9182918261016c565b0390f35b610109565b34610614576105f436600461021f565b6106106105ff610cef565b610607610103565b9182918261016c565b0390f35b610109565b346106495761062936600461021f565b610645610634610d04565b61063c610103565b918291826102d6565b0390f35b610109565b5190565b60209181520190565b60005b83811061066f575050906000910152565b80602091830151818501520161065e565b601f801991011690565b6106a96106b26020936106b7936106a08161064e565b93848093610652565b9586910161065b565b610680565b0190565b6106d1916020820191600081840391015261068a565b90565b34610704576106e436600461021f565b6107006106ef610e43565b6106f7610103565b918291826106bb565b0390f35b610109565b600080fd5b600090565b60001c90565b61072561072a91610713565b610233565b90565b6107379054610719565b90565b60007f212120576962626c7920576f62626c792054696d65792057696d657920212100910152565b61076f601f602092610652565b6107788161073a565b0190565b6107929060208101906000818303910152610762565b90565b1561079c57565b6107a4610103565b62461bcd60e51b8152806107ba6004820161077c565b0390fd5b6107ca6107cf91610713565b610573565b90565b6107dc90546107be565b90565b90565b90565b6107f96107f46107fe926107df565b6107e2565b610159565b90565b634e487b7160e01b600052601160045260246000fd5b61082361082991610159565b91610159565b90039060ff821161083657565b610801565b61084a61085091939293610113565b92610113565b820391821161085b57565b610801565b634e487b7160e01b600052601260045260246000fd5b61088261088891610113565b91610113565b908115610893570490565b610860565b6108ac6108a76108b192610113565b6107e2565b610159565b90565b6108bc61070e565b506108e3816108dc6108d66108d1600061072d565b610113565b91610113565b1015610795565b806108ff6108f96108f4600161072d565b610113565b91610113565b101561094e5761094661091f61094b92610919600061072d565b9061083b565b7f000000000000000000000000000000000000000000000000000000000000000090610876565b610898565b90565b5061096c61095c60026107d2565b61096660016107e5565b90610817565b90565b60207f6e65720000000000000000000000000000000000000000000000000000000000917f4f776e61626c653a207478206f726967696e206973206e6f7420746865206f7760008201520152565b6109ca6023604092610652565b6109d38161096f565b0190565b6109ed90602081019060008183039101526109bd565b90565b156109f757565b6109ff610103565b62461bcd60e51b815280610a15600482016109d7565b0390fd5b90610a5f91610a5a32610a54610a4e7f00000000000000000000000000000000000000000000000000000000000000006103ec565b916103ec565b146109f0565b610b88565b565b60001b90565b90610a7360ff91610a61565b9181191691161790565b610a91610a8c610a9692610159565b6107e2565b610159565b90565b90565b90610ab1610aac610ab892610a7d565b610a99565b8254610a67565b9055565b90610ac960001991610a61565b9181191691161790565b610ae7610ae2610aec92610113565b6107e2565b610113565b90565b90565b90610b07610b02610b0e92610ad3565b610aef565b8254610abc565b9055565b610b26610b21610b2b92610159565b6107e2565b610113565b90565b610b3d610b4391939293610113565b92610113565b91610b4f838202610113565b928184041490151715610b5e57565b610801565b610b72610b7891939293610113565b92610113565b8201809211610b8357565b610801565b610c2390610bd6610bcf610bca610ba3610c2a96859061083b565b7f000000000000000000000000000000000000000000000000000000000000000090610876565b610898565b6002610a9c565b610be1816000610af2565b610c1d610bee60026107d2565b610c187f000000000000000000000000000000000000000000000000000000000000000091610b12565b610b2e565b90610b63565b6001610af2565b565b90610c3691610a19565b565b600090565b610c45610c38565b5042610c8c610c86610c81610c5a600161072d565b7f000000000000000000000000000000000000000000000000000000000000000090610b63565b610113565b91610113565b101590565b610c99610c38565b5042610cb6610cb0610cab600161072d565b610113565b91610113565b1090565b600090565b610cc7610cba565b50610cd0610c3d565b610cea57610cdc610c91565b610ce557600190565b600090565b600290565b610cf761070e565b50610d01426108b4565b90565b610d0c610c38565b5042610d29610d23610d1e600161072d565b610113565b91610113565b101580610d34575b90565b5042610d7b610d75610d70610d49600161072d565b7f000000000000000000000000000000000000000000000000000000000000000090610b63565b610113565b91610113565b10610d31565b606090565b634e487b7160e01b600052604160045260246000fd5b90610da690610680565b810190811067ffffffffffffffff821117610dc057604052565b610d86565b905090565b610def610de692602092610ddd8161064e565b94858093610dc5565b9384910161065b565b0190565b91610e05610e119493610e0b93610dca565b90610dca565b90610dca565b90565b90610e4191939293610e35610e27610103565b958693602085019384610df3565b90810382520383610d9c565b565b610e4b610d81565b50610ed4610e787f0000000000000000000000000000000000000000000000000000000000000000611025565b610ea17f0000000000000000000000000000000000000000000000000000000000000000611025565b90610ecb7f0000000000000000000000000000000000000000000000000000000000000000611025565b90919091610e14565b90565b90565b610eee610ee9610ef392610ed7565b6107e2565b610113565b90565b90565b610f0d610f08610f1292610ef6565b6107e2565b610113565b90565b634e487b7160e01b600052603260045260246000fd5b60f81b90565b60ff60f81b1690565b610f4e610f49610f5392610ed7565b610f2b565b610f31565b90565b610f5f90610113565b6000198114610f6e5760010190565b610801565b90610f86610f7f610103565b9283610d9c565b565b67ffffffffffffffff8111610fa657610fa2602091610680565b0190565b610d86565b90610fbd610fb883610f88565b610f73565b918252565b369037565b90610fec610fd483610fab565b92602080610fe28693610f88565b9201910390610fc2565b565b6001610ffa9101610113565b90565b5190565b9061100b82610ffd565b81101561101d57600160209102010190565b610f15565b90565b61102d610d81565b506110386000610eda565b5b8061104d6110476020610ef9565b91610113565b10806110e1575b156110675761106290610f56565b611039565b9061107182610fc7565b9261107c6000610eda565b5b8061109061108a86610113565b91610113565b10156110d05782908060208110156110cb576110c6926110b0911a610f2b565b6110c08791839060001a92611001565b53610fee565b61107d565b610f15565b509290506110de9150611022565b90565b508181906020821015611112576110f8911a610f2b565b61110b6111056000610f3a565b91610f31565b1415611054565b610f1556fea26469706673582212200ddc76541d9709a29f7c815b21bfaf5c79457fc08f1ea39d58da99b142ca92e464736f6c634300081c0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_CREATION_DATE = "CREATION_DATE";

    public static final String FUNC_END_OF_WEEK_CRON = "END_OF_WEEK_CRON";

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

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RESET = "reset";

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

    public RemoteFunctionCall<String> END_OF_WEEK_CRON() {
        final Function function = new Function(FUNC_END_OF_WEEK_CRON, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> reset(BigInteger creationDate,
            BigInteger expirationDate) {
        final Function function = new Function(
                FUNC_RESET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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
            BigInteger expirationDate, BigInteger secondsInOneWeek, byte[] endOfWeekCronP1,
            byte[] endOfWeekCronP2, byte[] endOfWeekCronP3) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP1), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP2), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP3)));
        return deployRemoteCall(TheDoctor.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<TheDoctor> deploy(Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek, byte[] endOfWeekCronP1,
            byte[] endOfWeekCronP2, byte[] endOfWeekCronP3) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP1), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP2), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP3)));
        return deployRemoteCall(TheDoctor.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TheDoctor> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek, byte[] endOfWeekCronP1,
            byte[] endOfWeekCronP2, byte[] endOfWeekCronP3) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP1), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP2), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP3)));
        return deployRemoteCall(TheDoctor.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TheDoctor> deploy(Web3j web3j, TransactionManager transactionManager,
            BigInteger gasPrice, BigInteger gasLimit, BigInteger creationDate,
            BigInteger expirationDate, BigInteger secondsInOneWeek, byte[] endOfWeekCronP1,
            byte[] endOfWeekCronP2, byte[] endOfWeekCronP3) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP1), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP2), 
                new org.web3j.abi.datatypes.generated.Bytes32(endOfWeekCronP3)));
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
