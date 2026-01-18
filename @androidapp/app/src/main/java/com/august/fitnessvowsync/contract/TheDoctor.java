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
    public static final String BINARY = "6101006040523461006257610021610015610186565b949390939291926103fa565b610029610067565b61114a6104f982396080518181816103430152610a63015260a05181610e1c015260c05181610e45015260e05181610e6f015261114a90f35b61006d565b60405190565b600080fd5b601f801991011690565b634e487b7160e01b600052604160045260246000fd5b9061009c90610072565b810190811060018060401b038211176100b457604052565b61007c565b906100cc6100c5610067565b9283610092565b565b600080fd5b90565b6100df816100d3565b036100e657565b600080fd5b905051906100f8826100d6565b565b90565b610106816100fa565b0361010d57565b600080fd5b9050519061011f826100fd565b565b909160c0828403126101815761013a83600084016100eb565b9261014881602085016100eb565b9261015682604083016100eb565b9261017e6101678460608501610112565b936101758160808601610112565b9360a001610112565b90565b6100ce565b6101a461164380380380610199816100b9565b928339810190610121565b909192939495565b60001b90565b906101bf600019916101ac565b9181191691161790565b90565b6101e06101db6101e5926100d3565b6101c9565b6100d3565b90565b90565b906102006101fb610207926101cc565b6101e8565b82546101b2565b9055565b90565b61022261021d6102279261020b565b6101c9565b6100d3565b90565b634e487b7160e01b600052601260045260246000fd5b634e487b7160e01b600052601160045260246000fd5b610262610268916100d3565b916100d3565b908115610273570490565b61022a565b90565b61028f61028a61029492610278565b6101c9565b6100d3565b90565b6102a66102ac919392936100d3565b926100d3565b82039182116102b757565b610240565b60001c90565b90565b6102d16102d6916102bc565b6102c2565b90565b6102e390546102c5565b90565b60ff1690565b6103006102fb610305926100d3565b6101c9565b6102e6565b90565b9061031460ff916101ac565b9181191691161790565b61033261032d610337926102e6565b6101c9565b6102e6565b90565b90565b9061035261034d6103599261031e565b61033a565b8254610308565b9055565b60ff1690565b61036f610374916102bc565b61035d565b90565b6103819054610363565b90565b61039861039361039d926102e6565b6101c9565b6100d3565b90565b6103af6103b5919392936100d3565b926100d3565b916103c18382026100d3565b9281840414901517156103d057565b610240565b6103e46103ea919392936100d3565b926100d3565b82018092116103f557565b610240565b61047561046e6104696104596104ab9561045261044b61043a6104b29a61041f6104bd565b61042a8160006101eb565b610434600561020e565b90610256565b610445610e1061027b565b906104c8565b60046101eb565b8590610297565b61046360006102d9565b90610256565b6102ec565b600361033d565b6104808160016101eb565b6104a561048d6003610377565b6104a061049a60006102d9565b91610384565b6103a0565b906103d5565b60026101eb565b60a05260c05260e052565b33608052565b600090565b6104d06104c3565b50806104e46104de846100d3565b916100d3565b116000146104f157505b90565b90506104ee56fe60806040526004361015610013575b6106ee565b61001e6000356100fd565b806301a1a44e146100f85780633e6eb0c4146100f3578063480d01bb146100ee5780634e7c3065146100e95780638485eb35146100e45780638da5cb5b146100df5780638f5949f9146100da578063a6801cbd146100d5578063c1a287e2146100d0578063c29ae2a6146100cb578063d1e9ff11146100c6578063d80f9847146100c1578063ebfb7af6146100bc5763ee1ebc7f0361000e576106b9565b6105fe565b6105c9565b610594565b610523565b61048e565b61044a565b6103d4565b61039f565b61030c565b6102c7565b610284565b610227565b610182565b60e01c90565b60405190565b600080fd5b600080fd5b90565b61011f81610113565b0361012657565b600080fd5b9050359061013882610116565b565b90602082820312610154576101519160000161012b565b90565b61010e565b60ff1690565b61016890610159565b9052565b91906101809060006020850194019061015f565b565b346101b2576101ae61019d61019836600461013a565b610899565b6101a5610103565b9182918261016c565b0390f35b610109565b60009103126101c257565b61010e565b1c90565b90565b6101de9060086101e393026101c7565b6101cb565b90565b906101f191546101ce565b90565b61020160026000906101e6565b90565b61020d90610113565b9052565b919061022590600060208501940190610204565b565b34610257576102373660046101b7565b6102536102426101f4565b61024a610103565b91829182610211565b0390f35b610109565b151590565b61026a9061025c565b9052565b919061028290600060208501940190610261565b565b346102b4576102943660046101b7565b6102b061029f610967565b6102a7610103565b9182918261026e565b0390f35b610109565b6102c46000806101e6565b90565b346102f7576102d73660046101b7565b6102f36102e26102b9565b6102ea610103565b91829182610211565b0390f35b610109565b61030960016000906101e6565b90565b3461033c5761031c3660046101b7565b6103386103276102fc565b61032f610103565b91829182610211565b0390f35b610109565b7f000000000000000000000000000000000000000000000000000000000000000090565b60018060a01b031690565b61037990610365565b90565b61038590610370565b9052565b919061039d9060006020850194019061037c565b565b346103cf576103af3660046101b7565b6103cb6103ba610341565b6103c2610103565b91829182610389565b0390f35b610109565b34610404576103e43660046101b7565b6104006103ef6109a4565b6103f7610103565b9182918261026e565b0390f35b610109565b909160608284031261043f5761043c610425846000850161012b565b93610433816020860161012b565b9360400161012b565b90565b61010e565b60000190565b346104795761046361045d366004610409565b91610c8c565b61046b610103565b8061047581610444565b0390f35b610109565b61048b60046000906101e6565b90565b346104be5761049e3660046101b7565b6104ba6104a961047e565b6104b1610103565b91829182610211565b0390f35b610109565b634e487b7160e01b600052602160045260246000fd5b600311156104e357565b6104c3565b906104f2826104d9565b565b6104fd906104e8565b90565b610509906104f4565b9052565b919061052190600060208501940190610500565b565b34610553576105333660046101b7565b61054f61053e610c9e565b610546610103565b9182918261050d565b0390f35b610109565b60ff1690565b61056e90600861057393026101c7565b610558565b90565b90610581915461055e565b90565b6105916003600090610576565b90565b346105c4576105a43660046101b7565b6105c06105af610584565b6105b7610103565b9182918261016c565b0390f35b610109565b346105f9576105d93660046101b7565b6105f56105e4610cce565b6105ec610103565b9182918261016c565b0390f35b610109565b3461062e5761060e3660046101b7565b61062a610619610ce3565b610621610103565b9182918261026e565b0390f35b610109565b5190565b60209181520190565b60005b838110610654575050906000910152565b806020918301518185015201610643565b601f801991011690565b61068e61069760209361069c9361068581610633565b93848093610637565b95869101610640565b610665565b0190565b6106b6916020820191600081840391015261066f565b90565b346106e9576106c93660046101b7565b6106e56106d4610e0b565b6106dc610103565b918291826106a0565b0390f35b610109565b600080fd5b600090565b60001c90565b61070a61070f916106f8565b6101cb565b90565b61071c90546106fe565b90565b60007f212120576962626c7920576f62626c792054696d65792057696d657920212100910152565b610754601f602092610637565b61075d8161071f565b0190565b6107779060208101906000818303910152610747565b90565b1561078157565b610789610103565b62461bcd60e51b81528061079f60048201610761565b0390fd5b6107af6107b4916106f8565b610558565b90565b6107c190546107a3565b90565b90565b90565b6107de6107d96107e3926107c4565b6107c7565b610159565b90565b634e487b7160e01b600052601160045260246000fd5b61080861080e91610159565b91610159565b90039060ff821161081b57565b6107e6565b61082f61083591939293610113565b92610113565b820391821161084057565b6107e6565b634e487b7160e01b600052601260045260246000fd5b61086761086d91610113565b91610113565b908115610878570490565b610845565b61089161088c61089692610113565b6107c7565b610159565b90565b6108a16106f3565b506108c8816108c16108bb6108b66001610712565b610113565b91610113565b101561077a565b806108e46108de6108d96002610712565b610113565b91610113565b101561091c57610914610904610919926108fe6001610712565b90610820565b61090e6000610712565b9061085b565b61087d565b90565b5061093a61092a60036107b7565b61093460016107ca565b906107fc565b90565b600090565b61095161095791939293610113565b92610113565b820180921161096257565b6107e6565b61096f61093d565b504261099f6109996109946109846002610712565b61098e6004610712565b90610942565b610113565b91610113565b101590565b6109ac61093d565b50426109c96109c36109be6002610712565b610113565b91610113565b1090565b60007f466f7262696464656e2e00000000000000000000000000000000000000000000910152565b610a02600a602092610637565b610a0b816109cd565b0190565b610a2590602081019060008183039101526109f5565b90565b15610a2f57565b610a37610103565b62461bcd60e51b815280610a4d60048201610a0f565b0390fd5b90610a989291610a9332610a8d610a877f0000000000000000000000000000000000000000000000000000000000000000610370565b91610370565b14610a28565b610bda565b565b60001b90565b90610aad60001991610a9a565b9181191691161790565b610acb610ac6610ad092610113565b6107c7565b610113565b90565b90565b90610aeb610ae6610af292610ab7565b610ad3565b8254610aa0565b9055565b90565b610b0d610b08610b1292610af6565b6107c7565b610113565b90565b90565b610b2c610b27610b3192610b15565b6107c7565b610113565b90565b90610b4060ff91610a9a565b9181191691161790565b610b5e610b59610b6392610159565b6107c7565b610159565b90565b90565b90610b7e610b79610b8592610b4a565b610b66565b8254610b34565b9055565b610b9d610b98610ba292610159565b6107c7565b610113565b90565b610bb4610bba91939293610113565b92610113565b91610bc6838202610113565b928184041490151715610bd557565b6107e6565b610c4d610c46610c41610c31610c8395610c2a610c23610c12610c8a9a610c02816000610ad6565b610c0c6005610af9565b9061085b565b610c1d610e10610b18565b90610ea4565b6004610ad6565b8590610820565b610c3b6000610712565b9061085b565b61087d565b6003610b69565b610c58816001610ad6565b610c7d610c6560036107b7565b610c78610c726000610712565b91610b89565b610ba5565b90610942565b6002610ad6565b565b90610c979291610a51565b565b600090565b610ca6610c99565b50610caf610967565b610cc957610cbb6109a4565b610cc457600190565b600090565b600290565b610cd66106f3565b50610ce042610899565b90565b610ceb61093d565b5042610d08610d02610cfd6002610712565b610113565b91610113565b101580610d13575b90565b5042610d43610d3d610d38610d286002610712565b610d326004610712565b90610942565b610113565b91610113565b10610d10565b606090565b634e487b7160e01b600052604160045260246000fd5b90610d6e90610665565b810190811067ffffffffffffffff821117610d8857604052565b610d4e565b905090565b610db7610dae92602092610da581610633565b94858093610d8d565b93849101610640565b0190565b91610dcd610dd99493610dd393610d92565b90610d92565b90610d92565b90565b90610e0991939293610dfd610def610103565b958693602085019384610dbb565b90810382520383610d64565b565b610e13610d49565b50610e9c610e407f0000000000000000000000000000000000000000000000000000000000000000611022565b610e697f0000000000000000000000000000000000000000000000000000000000000000611022565b90610e937f0000000000000000000000000000000000000000000000000000000000000000611022565b90919091610ddc565b90565b600090565b610eac610e9f565b5080610ec0610eba84610113565b91610113565b11600014610ecd57505b90565b9050610eca565b90565b610eeb610ee6610ef092610ed4565b6107c7565b610113565b90565b90565b610f0a610f05610f0f92610ef3565b6107c7565b610113565b90565b634e487b7160e01b600052603260045260246000fd5b60f81b90565b60ff60f81b1690565b610f4b610f46610f5092610ed4565b610f28565b610f2e565b90565b610f5c90610113565b6000198114610f6b5760010190565b6107e6565b90610f83610f7c610103565b9283610d64565b565b67ffffffffffffffff8111610fa357610f9f602091610665565b0190565b610d4e565b90610fba610fb583610f85565b610f70565b918252565b369037565b90610fe9610fd183610fa8565b92602080610fdf8693610f85565b9201910390610fbf565b565b6001610ff79101610113565b90565b5190565b9061100882610ffa565b81101561101a57600160209102010190565b610f12565b90565b61102a610d49565b506110356000610ed7565b5b8061104a6110446020610ef6565b91610113565b10806110de575b156110645761105f90610f53565b611036565b9061106e82610fc4565b926110796000610ed7565b5b8061108d61108786610113565b91610113565b10156110cd5782908060208110156110c8576110c3926110ad911a610f28565b6110bd8791839060001a92610ffe565b53610feb565b61107a565b610f12565b509290506110db915061101f565b90565b50818190602082101561110f576110f5911a610f28565b6111086111026000610f37565b91610f2e565b1415611051565b610f1256fea2646970667358221220d6c8ece37582c885ee8dc8d0edc26f6d95f06c7a73049d5d8eb217d3a7366f2364736f6c634300081c0033";

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
            BigInteger expirationDate, BigInteger secondsInOneWeek) {
        final Function function = new Function(
                FUNC_RESET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(creationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(expirationDate), 
                new org.web3j.abi.datatypes.generated.Uint256(secondsInOneWeek)), 
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
