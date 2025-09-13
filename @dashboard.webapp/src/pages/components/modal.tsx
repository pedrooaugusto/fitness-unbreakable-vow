import React, { type ReactElement } from "react";

export default function Modal(props: { Content: ReactElement | null, button: ReactElement | undefined, title: string | null, closeModal?: () => void }) {
    if (!props.Content) return null;

    const Content = props.Content;

    return (
        <div className="modal">
            <div className="modal-content">
                <h2>{props.title || 'Modal Title'}</h2>
                {Content}
            </div>
        </div>
    );
}

export interface WithModalProps {
    openModal: (Content: ReactElement, title: string, button?: ReactElement) => void;
    closeModal: () => void;
}

export function WithModal<T extends WithModalProps>(Component: React.ComponentType<T>) {
    return function ModalWrapper(props: T) {
        const [content, setContent] = React.useState<ReactElement | null>(null);
        const [button, setButton] = React.useState<ReactElement | undefined>();
        const [title, setTitle] = React.useState<string | null>(null);

        const openModal = (Content: ReactElement, title: string, button?: ReactElement) => {
            setContent(Content);
            setTitle(title);
            setButton(button);
            document.body.style.overflow = 'hidden';
        }

        const closeModal = () => {
            setContent(null);
            setTitle(null);
            setButton(undefined);
            document.body.style.overflow = 'auto';
        }

        return (
            <div>
                <Component {...props } openModal={openModal} closeModal={closeModal} />
                <div className={`modal-wrapper ${content ? "open" : ""}`}>
                    <Modal Content={content} closeModal={closeModal} title={title} button={button} />
                </div>
            </div>
        );
    }
}