/**
 * Mock server used for development only,
 * real backend in handler.ts
 */
import express, { Request, Response } from "express";
import cors from "cors";
import { handler } from "./ContractOverviewFunction";

const app = express();
const port = process.env.PORT || 3000;

app.use(cors())

const cache: Record<string, any> = {};
app.get("/api/overview", async (req: Request, res: Response) => {
    const r = cache['r'] ? cache['r'] : await handler(req as any);

    //cache['r'] = r;

    res.send(r);
});

app.listen(port, () => {
    console.log(`🚀 Server is running at http://localhost:${port}`);
});