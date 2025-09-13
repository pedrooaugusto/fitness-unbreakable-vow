/**
 * Mock server used for development only,
 * real backend in handler.ts
 */
import express, { Request, Response } from "express";
import cors from "cors";
import * as ContractOverviewFunction from "./ContractOverviewFunction";
import * as WeekDetailsFunction from './WeekDetailsFunction';

const app = express();
const port = process.env.PORT || 3000;

app.use(cors())

const cache: Record<string, any> = {};
app.get("/api/overview", async (req: Request, res: Response) => {
    const response = await ContractOverviewFunction.handler(req as any);

    response.body = JSON.parse(response.body);

    res.send(response);
});

app.get("/api/week-details/:weekIndex", async (req: Request, res: Response) => {
    const response = await WeekDetailsFunction.handler({ pathParameters: { weekIndex: req.params.weekIndex } } as any) ;

    response.body = JSON.parse(response.body);

    res.send(response);
});

app.listen(port, () => {
    console.log(`🚀 Server is running at http://localhost:${port}`);
});