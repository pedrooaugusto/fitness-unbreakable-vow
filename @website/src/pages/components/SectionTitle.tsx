import type { ReactElement } from "react";

export const SectionTitle = (props: {
    icon: ReactElement;
    text: string;
    subtext?: ReactElement;
    openModal?: () => void;
}) => (
    <div className="title" onClick={props.openModal}>
        <h2>
            <span className="icon">{props.icon}</span>
            {props.text}
        </h2>
        <div className="subtext">{props.subtext}</div>
    </div>
);