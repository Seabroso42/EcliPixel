import os
import streamlit as st
import numpy as np
import cv2
from io import BytesIO
from PIL import Image
import requests

st.markdown(
    """
    <style>
    body {
        background-color: #6E6454;
    }
    .stApp {
        background-color: #bca66c;
    }
    .stSidebar {
        background-color: #6E6454;
    }
    .img:hover::after {
            content: attr(alt);
        }
    </style>
    """,
    unsafe_allow_html=True
)



st.set_page_config(layout="wide",
                    # page_title="EcliPixel",
                    initial_sidebar_state="collapsed")

# ================= Header =================
col1, col2 = st.columns([1, 9])
with col1:
    st.image("assets/logo.png", width=200) 
    # st.markdown("### EcliPixel ")


# ================= Layout =================
left_col, right_col = st.columns([1.2, 2])

with left_col:
    st.title("")

    square_size = 700
    image = np.zeros((square_size, square_size, 3), dtype=np.uint8)
    image[:, :] = [69, 69, 69]

    uploaded_file = st.file_uploader("Escolha uma imagem", type=["png", "jpeg", "jpg"], label_visibility="collapsed", key="file_uploader")

    # Load the overlay image (ensure it has alpha channel)
    overlay_path = "assets/Add Image.png"
    overlay = cv2.imread(overlay_path, cv2.IMREAD_UNCHANGED)
    
    if overlay is not None:
        max_overlay_size = int(square_size * 0.5)
        h, w = overlay.shape[:2]
        scale = min(max_overlay_size / h, max_overlay_size / w, 1.0)
        new_size = (int(w * scale), int(h * scale))
        overlay_resized = cv2.resize(overlay, new_size, interpolation=cv2.INTER_AREA)

        y_offset = (square_size - overlay_resized.shape[0]) // 2
        x_offset = (square_size - overlay_resized.shape[1]) // 2

        if overlay_resized.shape[2] == 4:
            alpha = overlay_resized[:, :, 3] / 255.0
            for c in range(3):
                image[y_offset:y_offset+overlay_resized.shape[0], x_offset:x_offset+overlay_resized.shape[1], c] = (
                    alpha * overlay_resized[:, :, c] +
                    (1 - alpha) * image[y_offset:y_offset+overlay_resized.shape[0], x_offset:x_offset+overlay_resized.shape[1], c]
                ).astype(np.uint8)
        else:
            image[y_offset:y_offset+overlay_resized.shape[0], x_offset:x_offset+overlay_resized.shape[1], :] = overlay_resized[:, :, :3]

    if uploaded_file is not None:
        st.image(uploaded_file, caption="Imagem original", use_column_width=True)
    else:
        st.image(image, caption="", use_column_width=True)

    
    col1, col2 = st.columns([0.5, 1])
    with col1:
        col1a,col1b = st.columns([1, 1])
        square_size = 100
        
        with col1a:
            
            def overlay_centered(square_size, img_path):
                bg = np.zeros((square_size, square_size, 3), dtype=np.uint8)
                bg[:, :] = [255,255, 255]

                img = cv2.imread(img_path, cv2.IMREAD_UNCHANGED)
                if img is None:
                    return bg

                h, w = img.shape[:2]
                scale = min(square_size / h, square_size / w, 1.0)
                new_size = (int(w * scale), int(h * scale))
                img_resized = cv2.resize(img, new_size, interpolation=cv2.INTER_AREA)

                y_offset = (square_size - img_resized.shape[0]) // 2
                x_offset = (square_size - img_resized.shape[1]) // 2

                if img_resized.shape[2] == 4:
                    alpha = img_resized[:, :, 3] / 255.0
                    for c in range(3):
                        bg[y_offset:y_offset+img_resized.shape[0], x_offset:x_offset+img_resized.shape[1], c] = (
                            alpha * img_resized[:, :, c] +
                            (1 - alpha) * bg[y_offset:y_offset+img_resized.shape[0], x_offset:x_offset+img_resized.shape[1], c]
                        ).astype(np.uint8)
                else:
                    bg[y_offset:y_offset+img_resized.shape[0], x_offset:x_offset+img_resized.shape[1], :] = img_resized[:, :, :3]
                bg = cv2.cvtColor(bg, cv2.COLOR_BGR2RGB)
                return bg

            st.image(overlay_centered(square_size, "assets/Group 17.png"), use_container_width=False) 

        with col1b:
            st.markdown(
                """
                <style>
                .square-btn {
                    width: 100px !important;
                    height: 100px !important;
                    font-size: 16px;
                    border: none;
                    border-radius: 8px;
                    cursor: pointer;
                    margin-bottom: 10px;
                }
                </style>
                <button class="square-btn" onclick="window.dispatchEvent(new Event('multithread '));">Multithread/Simples</button>
                """,
                unsafe_allow_html=True
            )

    with col2:
        width, height = 420, 100
        image = np.zeros((height, width, 3), dtype=np.uint8)
        image[:, :] = [255, 255, 255]
        st.image(image, caption="")

    with right_col:
        st.title("Visualizador de resultados")

    if uploaded_file is not None:
        col1, col2 = st.columns(2)

        with col1:
            if st.button("Binarizar"):
                files = {"file": uploaded_file.getvalue()}
                response = requests.post("http://localhost:8080/binarizar", files=files)
                if response.status_code == 200:
                    img = Image.open(BytesIO(response.content))
                    st.image(img, caption="Imagem binarizada")
                else:
                    st.error("Erro na binarização")

        with col2:
            if st.button("Ver histograma"):
                files = {"file": uploaded_file.getvalue()}
                response = requests.post("http://localhost:8080/histograma", files=files)
                if response.status_code == 200:
                    hist_data = response.json()
                    st.bar_chart(hist_data)
                else:
                    st.error("Erro ao obter histograma")
