# **FitVow Smart Contract**

This smart contract creates a self-enforced health and wellness commitment backed by financial penalties and decentralized accountability.

---

## **1. Commitment Overview**

- **Duration:** 3 months (12 weeks)  
- **Stake:** $150 USD (or equivalent in a stablecoin such as USDC), locked within the smart contract.  

This stake acts as collateral to incentivize goal completion. It will be gradually reduced if health and habit goals are not met.  

---

## **2. Weekly Health Goals & Verification**

To remain in good standing each week, the **Pledger** must complete at least **two** of the following verifiable obligations:

- **Run:** A running session of at least 2 km (measured via Smart Watch).  
- **Healthy Sleep:** Log at least two nights with 7h30m or more of sleep (measured via Smart Watch).  
- **Gym Visit:** Verified presence at a registered gym (via Android Geofence).  

### **Data Integrity & Verification Process**

- Metrics are collected via the **FitVow Android App**.  
- Each record is signed on-device with a hardware-protected private key (Android Keystore / TEE).  
- Signed records are submitted to a **PhysicalActivityOracle Contract**, which validates the signatures and makes the data available to the vow contract.  
- A **sign-and-forget build mechanism** ensures app authenticity:  
  - APKs are signed once with an ephemeral key that is immediately discarded.  
  - Because the signing key is unrecoverable, new modified builds cannot replace the installed app.  
  - Reinstallation would erase all original keys and records, preventing fraudulent continuation.  

---

## **3. Enforcement & Penalty Mechanism**

If the contract fails to verify that weekly obligations were completed:

- **Fine:** $8 USD (or equivalent) is deducted from the staked balance.  
- **Distribution:**  
  - If the `enforceAgreement()` function is called by an individual user, the fine is split equally between the caller (the **Enforcer**) and the **Giveth Charity Foundation** at  
    [`0x6e8873085530406995170Da467010565968C7C62`](https://etherscan.io/address/0x6e8873085530406995170Da467010565968C7C62).  
  - If enforcement is performed by the designated **Upkeeper** (e.g., Chainlink Automation), **100% of the fine** is routed to the Charity.  

---

## **4. Contract Completion & Fund Release**

At the end of the 3-month term:

- Any remaining staked funds are automatically released and returned to the **Pledger’s wallet**.  
- No further enforcement or obligations remain.  

---

## **5. Trust & Decentralization Model**

The **FitVow Agreement** operates under a decentralized trust model:

- **Immutable Logic:** All rules are codified in the smart contract, transparently auditable on-chain.  
- **Tamper Resistance:** Records are signed by the app using hardware-protected keys, validated on-chain by the Oracle.  
- **Sign-and-Forget Distribution:** Ephemeral APK signing prevents the developer from re-issuing modified versions with the same identity.  
- **Open Enforcement:** Any address may invoke `enforceAgreement()` against a breaching Pledger, decentralizing accountability.  
- **Charity Guarantee:** The Upkeeper ensures enforcement even if no enforcer acts, routing fines fully to Charity.  
