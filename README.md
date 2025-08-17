## TODO
set appropirate gas amount in contracts and client
handle contract expiration gracefuly in the ui

## **FitnessUnbreakableVow Smart Contract**

This smart contract creates a self-enforced physical fitness commitment backed by financial penalties and decentralized accountability.

---

### **1. Commitment Overview**

* **Duration:** 3 months (12 weeks)
* **Stake:** \$150 USD (or equivalent in a stablecoin such as USDC), locked within the smart contract.

This stake acts as collateral to incentivize goal completion. It will be gradually reduced if activity goals are not met.

---

### **2. Weekly Fitness Goals & Verification**

To remain in good standing each week, the contract owner must complete at least **two** of the following verifiable goals:

* **Run:** A running session of at least 2 km (measured via Smart Watch).
* **Healthy Sleep:** Log at least two nights with 7 hours and 30 minutes or more of sleep (measured via Smart Watch).
* **Gym Visit:** Verified physical presence at a registered gym (verified via Android Geofence).

#### **Data Integrity & Verification Process**

* Metrics are collected via a dedicated Android app.
* The app is safeguarded against tampering through validation using the **Google Play Integrity API**.
* Verified data is submitted to an **Oracle Contract**, which acts as a tamper-resistant record store accessible to this and other smart contracts.
* For why this works read: https://chatgpt.com/share/686c6c1d-e19c-800f-b26d-721d2970aef4

---

### **3. Enforcement & Penalty Mechanism**

If the contract fails to verify that weekly goals were completed:

* **Penalty:** \$8 USD (or equivalent) is deducted from the staked balance.
* **Recipient:**

  * If the `enforceTerms()` function is called by an individual user, they receive the penalty as a reward for enforcement.
  * If enforcement is performed by a designated third-party "upkeeper" (e.g., automation services like Chainlink Keepers), the penalty is instead routed to the [Giveth Charity Foundation](https://giveth.io/project/Giveth-Matching-Pool-0) at
    [`0x6e8873085530406995170Da467010565968C7C62`](https://etherscan.io/address/0x6e8873085530406995170Da467010565968C7C62), ensuring they contribute to a positive cause rather than being returned to the contract's owner.


This structure encourages community accountability and automates consequences.

---

### **4. Contract Completion & Fund Release**

At the end of the 3-month term:

* Any remaining staked funds are automatically released and returned to the contract owner’s wallet.
* No additional user action is required.

---

### 5. Trust & Decentralization Model

The **FitnessUnbreakableVow** operates on a trust model that leverages decentralization:

* **Smart Contract Trust:** The core logic of the vow is immutable and transparently verifiable on the blockchain.
* **Data Integrity:** Trust in activity data relies on the integrity of the Android app and its attestation via the Google Play Integrity API.
* **Oracle Dependency:** The contract depends on the designated Oracle Contract for verified activity records.
* **Automated Enforcement:** The "anyone-can-call" `enforceTerms()` mechanism, combined with upkeepers, decentralizes the monitoring and enforcement process.

