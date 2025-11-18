// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;
import { Environment } from './Types.sol';

struct RegistrationParams {
    string name;
    bytes encryptedEmail;
    address upkeepContract;
    uint32 gasLimit;
    address adminAddress;
    uint8 triggerType;
    bytes checkData;
    bytes triggerConfig;
    bytes offchainConfig;
    uint96 amount;
}

interface LinkTokenManager {
    function approve(address spender, uint256 amount) external returns (bool);
    function transfer(address to, uint256 amount) external returns (bool);
    function transferFrom(address _from, address _to, uint256 _value) external returns (bool);
}

interface CronUpkeeperFactory {
    function encodeCronJob(address target, bytes memory handler, string memory cronString) external view returns (bytes memory);
    function encodeCronString(string memory cronString) external pure returns (bytes memory);
    function newCronUpkeepWithJob(bytes memory encodedJob) external;
}

/// @dev Source code can be found at:
/// https://github.com/smartcontractkit/chainlink-evm/.../AutomationRegistrar2_1.sol
interface UpkeeperRegistrar {
    function registerUpkeep(RegistrationParams memory params) external returns (uint256);   
}

/// @dev https://sepolia.arbiscan.io/address/0x8194399b3f11fca2e8ccefc4c9a658c61b8bf412#code#F8#L11
interface UpkeeperRegistry {
  function cancelUpkeep(uint256 id) external;
  function addFunds(uint256 id, uint96 amount) external;
  function withdrawFunds(uint256 id, address to) external;
}

interface Upkeeper {
    function updateCronJob(uint256 id, address newTarget, bytes memory newHandler, bytes memory newEncodedCronSpec) external;
}

abstract contract UpkeeperManager {
    /**
     * @notice Chainlink Upkeeper address. The Upkeeper is responsible
     * for automatically calling this contract once a week.
     * @dev It uses Chainlink Automation with a cron job to call the
     * `#enforceAgreement()` function once a week.
     */
    Upkeeper public CHAINLINK_UPKEEPER_ADDRESS;
    /**
     * @notice Chainlink Upkeepr ID. The internal ID of the Upkeeper
     * in Chainlink systems.
     * @dev This ID might be used to see the Upkeepr details in
     * Chainlink on the URL: https://automation.chain.link/arbitrum/${ID}.
     */
    uint256 public CHAINLINK_UPKEEPER_ID;

    LinkTokenManager private immutable linkToken;
    CronUpkeeperFactory private immutable upkeeperFactory;
    UpkeeperRegistrar private immutable upkeeperRegistrar;
    UpkeeperRegistry private immutable upkeeperRegistry;

    constructor() {
        if (Environment.isArbitrum()) {
            linkToken = LinkTokenManager(0xf97f4df75117a78c1A5a0DBb814Af92458539FB4);
            upkeeperFactory = CronUpkeeperFactory(0x96CbA89D87199F021DA22313c8b1f6B71A541f52);
            upkeeperRegistrar = UpkeeperRegistrar(0x86EFBD0b6736Bed994962f9797049422A3A8E8Ad);
            upkeeperRegistry = UpkeeperRegistry(0x37D9dC70bfcd8BC77Ec2858836B923c560E891D1);
        } else {
            // asume arbitrum sepolia
            linkToken = LinkTokenManager(0xb1D4538B4571d411F07960EF2838Ce337FE1E80E);
            upkeeperFactory = CronUpkeeperFactory(0xf155d88C61F59c7472C3f48D5b2805b7EdEd43DB);
            upkeeperRegistrar = UpkeeperRegistrar(0x881918E24290084409DaA91979A30e6f0dB52eBe);
            upkeeperRegistry = UpkeeperRegistry(0x8194399B3f11fcA2E8cCEfc4c9A658c61B8Bf412);
        }
    }

    function _createUpkeeper(string memory cronInternalSpec) internal {
        bytes memory encodedJob = upkeeperFactory.encodeCronJob(address(this), hex"3d3131f3", cronInternalSpec);

        upkeeperFactory.newCronUpkeepWithJob(encodedJob);
    }

    function _configureUpkeeper(address upkeeper, uint256 linkFunding, string memory cronInternalSpec) internal {
        CHAINLINK_UPKEEPER_ADDRESS = Upkeeper(upkeeper);

        linkToken.transferFrom(msg.sender, address(this), linkFunding);

        linkToken.approve(address(upkeeperRegistrar), linkFunding);

        RegistrationParams memory params = RegistrationParams({
            name: "FitVow-Auto-Enforcer",
            encryptedEmail: hex"",
            upkeepContract: upkeeper,
            gasLimit: 400000,
            adminAddress: address(this),
            triggerType: 0,
            checkData: hex"",
            triggerConfig: hex"",
            offchainConfig: hex"",
            amount: uint96(linkFunding)
        });

        CHAINLINK_UPKEEPER_ID = upkeeperRegistrar.registerUpkeep(params);
        CHAINLINK_UPKEEPER_ADDRESS.updateCronJob(1, params.adminAddress, hex"3d3131f3", upkeeperFactory.encodeCronString(cronInternalSpec));
    }

    function _cancelUpkeeper() internal returns (bool) {
        if (CHAINLINK_UPKEEPER_ID == 0) return false;

        try upkeeperRegistry.cancelUpkeep(CHAINLINK_UPKEEPER_ID) {
            return true;
        } catch {
            return false;
        }
    }

    function _withdrawUpkeeperFunds() internal {
        upkeeperRegistry.withdrawFunds(CHAINLINK_UPKEEPER_ID, msg.sender);
    }
}